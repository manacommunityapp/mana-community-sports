package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Role;

import com.manacommunity.sports.exception.CsvParseException;
import com.manacommunity.sports.exception.InvalidFileUploadException;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.SportsEventRegistration;
import com.manacommunity.sports.repository.PlayerCategoryRepository;
import com.manacommunity.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SportsEventCsvImportService {

    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final PlayerCategoryRepository categoryRepo;

    public record ImportResult(int imported, int skipped, List<String> skippedReasons) {}

    /**
     * Validates an uploaded CSV before parsing: non-empty, within the size cap, and a
     * genuine .csv. The filename is reduced to its base name first so a crafted name
     * like {@code ../../etc/passwd.csv} can never be used for path traversal (we never
     * touch the filesystem, but this keeps the value safe if it is ever logged/echoed).
     * Throws {@link IllegalArgumentException} → 400 on any violation.
     */
    private void validateCsvUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException("No file was uploaded, or the file is empty.");
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw new InvalidFileUploadException("CSV file is too large (max 5 MB).");
        }
        String raw = file.getOriginalFilename();
        String name = raw == null ? "" : raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim().toLowerCase(); // strip any path
        if (!name.endsWith(".csv")) {
            throw new InvalidFileUploadException("Only .csv files are accepted.");
        }
        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.equals("text/csv")
                && !contentType.equals("text/plain")
                && !contentType.equals("application/vnd.ms-excel")
                && !contentType.equals("application/octet-stream")) {
            throw new InvalidFileUploadException("Unsupported file type: " + contentType + " (expected CSV).");
        }
    }

    /** Hard cap independent of the multipart limit (defence in depth). */
    private static final long MAX_CSV_BYTES = 5L * 1024 * 1024;

    @Transactional
    public ImportResult importRegistrations(Long eventId, MultipartFile file) {
        validateCsvUpload(file);

        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        int imported = 0;
        List<String> skippedReasons = new ArrayList<>();

        // Pre-load dedup keys (name + email + flat number) for this event; also track keys added
        // during this import so duplicates within the same file are caught too.
        java.util.Set<String> seenKeys = new java.util.HashSet<>();
        regRepo.findByEventId(eventId).forEach(r ->
                seenKeys.add(dupKey(r.getPlayerName(), r.getEmail(), r.getFlatNumber())));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (isHeader) { isHeader = false; continue; }
                if (line.isBlank()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 1 || cols[0].isBlank()) {
                    skippedReasons.add("Row " + lineNum + ": empty player name");
                    continue;
                }

                // Column order: Player Name, Email, Category, Age, Flat Number, Relation, Primary Role
                String playerName = cols[0].trim();
                String email = cols.length > 1 ? cols[1].trim() : null;
                String categoryName = cols.length > 2 ? cols[2].trim() : "";
                String ageStr = cols.length > 3 ? cols[3].trim() : "";
                String flatNumber = cols.length > 4 ? cols[4].trim() : null;
                String relation = cols.length > 5 ? cols[5].trim() : null;
                String role = cols.length > 6 ? cols[6].trim() : null;

                // Duplicate check: name + email + flat number (case-insensitive, blank-safe)
                String key = dupKey(playerName, email, flatNumber);
                if (seenKeys.contains(key)) {
                    skippedReasons.add("Row " + lineNum + ": '" + playerName + "' duplicate (name + email + flat number)");
                    continue;
                }

                // Max participants guard
                if (regRepo.countByEventId(eventId) >= event.getMaxParticipants()) {
                    skippedReasons.add("Row " + lineNum + ": event is full (max " + event.getMaxParticipants() + ")");
                    break;
                }

                // Resolve category
                PlayerCategory category = null;
                if (!categoryName.isEmpty()) {
                    category = categoryRepo.findByNameIgnoreCase(categoryName).orElse(null);
                    if (category == null) {
                        skippedReasons.add("Row " + lineNum + ": unknown category '" + categoryName + "'");
                        continue;
                    }
                }

                // Parse age
                Integer age = null;
                if (!ageStr.isEmpty()) {
                    try { age = Integer.parseInt(ageStr); } catch (NumberFormatException ignored) {}
                }

                // Derive matchType from category name
                SportsEvent.MatchFormat matchType = deriveMatchType(categoryName);

                SportsEventRegistration reg = SportsEventRegistration.builder()
                        .event(event)
                        .user(null)
                        .category(category)
                        .matchType(matchType)
                        .playerName(playerName)
                        .email(email)
                        .flatNumber(flatNumber)
                        .relation(relation)
                        .age(age)
                        .role(role)
                        .status(SportsEventRegistration.RegistrationStatus.REGISTERED)
                        .registeredAt(LocalDateTime.now())
                        .build();

                regRepo.save(reg);
                seenKeys.add(key);
                imported++;
            }
        } catch (Exception e) {
            throw new CsvParseException("Failed to parse CSV: " + e.getMessage(), e);
        }

        return new ImportResult(imported, skippedReasons.size(), skippedReasons);
    }

    /** Normalized dedup key: name + email + flat number (case-insensitive, blank-safe). */
    private static String dupKey(String name, String email, String flat) {
        return norm(name) + "|" + norm(email) + "|" + norm(flat);
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private SportsEvent.MatchFormat deriveMatchType(String categoryName) {
        if (categoryName == null) return SportsEvent.MatchFormat.SINGLES;
        String lower = categoryName.toLowerCase();
        if (lower.contains("mixed")) return SportsEvent.MatchFormat.MIXED_DOUBLES;
        if (lower.contains("double")) return SportsEvent.MatchFormat.DOUBLES;
        if (lower.contains("team")) return SportsEvent.MatchFormat.TEAM;
        return SportsEvent.MatchFormat.SINGLES;
    }
}


