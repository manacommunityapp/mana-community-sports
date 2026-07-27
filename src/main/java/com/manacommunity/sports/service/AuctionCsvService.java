package com.manacommunity.sports.service;

import com.manacommunity.sports.exception.CsvParseException;
import com.manacommunity.sports.exception.InvalidFileUploadException;
import com.manacommunity.sports.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.AuctionConfig;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.repository.AuctionConfigRepository;
import com.manacommunity.sports.repository.AuctionPlayerRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionCsvService {

    private final AuctionPlayerRepository playerRepo;
    private final AuctionConfigRepository configRepo;

    private static final long MAX_CSV_BYTES = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 2000;
    private static final int MAX_FIELD_LENGTH = 200;

    public record UploadResult(int imported, int skipped, List<String> skippedReasons) {}

    /**
     * CSV Format Expected:
     * player_name,category,role,age,base_price,matches,runs,wickets
     * Ravi Varma,BATSMEN,Right-Hand Bat,26,1000,42,1200,5
     * Ajay Kumar,BOWLERS,Right-Arm Fast,28,1000,38,200,52
     */
    @Transactional
    public UploadResult uploadPlayersFromFile(Long configId, MultipartFile file) {
        validateCsvUpload(file);

        AuctionConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig", configId));

        int imported = 0;
        List<String> skippedReasons = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();

        playerRepo.findByConfigId(configId)
            .forEach(p -> seenNames.add(p.getPlayerName().trim().toLowerCase()));

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int queueOrder = playerRepo.findByConfigId(configId).size() + 1;
            boolean isHeader = true;
            int lineNum = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (isHeader) { isHeader = false; continue; }
                if (line.isBlank()) continue;

                if (lineNum > MAX_ROWS + 1) {
                    skippedReasons.add("Row " + lineNum + ": max row limit (" + MAX_ROWS + ") reached");
                    break;
                }

                String[] cols = line.split(",", -1);
                if (cols.length < 5) {
                    skippedReasons.add("Row " + lineNum + ": fewer than 5 columns");
                    continue;
                }

                String playerName = sanitizeField(cols[0]);
                if (playerName.isEmpty()) {
                    skippedReasons.add("Row " + lineNum + ": empty player name");
                    continue;
                }

                String nameKey = playerName.toLowerCase();
                if (seenNames.contains(nameKey)) {
                    skippedReasons.add("Row " + lineNum + ": duplicate player '" + playerName + "'");
                    continue;
                }

                String category = sanitizeField(cols[1]).toUpperCase();
                if (category.isEmpty()) {
                    skippedReasons.add("Row " + lineNum + ": empty category");
                    continue;
                }

                String role = cols.length > 2 ? sanitizeField(cols[2]) : null;

                Integer age = parseIntSafe(cols[3].trim());
                if (age != null && (age < 1 || age > 120)) {
                    skippedReasons.add("Row " + lineNum + ": invalid age " + age);
                    continue;
                }

                Integer basePrice = parseIntSafe(cols[4].trim());
                if (basePrice == null || basePrice < 0) {
                    skippedReasons.add("Row " + lineNum + ": invalid base price");
                    continue;
                }

                AuctionPlayer player = AuctionPlayer.builder()
                    .config(config)
                    .playerName(playerName)
                    .category(category)
                    .playerRole(role)
                    .age(age)
                    .basePrice(basePrice)
                    .statsJson(buildStats(cols, lineNum, skippedReasons))
                    .queueOrder(queueOrder++)
                    .status(AuctionPlayer.PlayerStatus.QUEUED)
                    .build();

                playerRepo.save(player);
                seenNames.add(nameKey);
                imported++;
            }
        } catch (InvalidFileUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new CsvParseException("Failed to parse player CSV: " + e.getMessage(), e);
        }

        log.info("CSV upload for config {}: imported={}, skipped={}", configId, imported, skippedReasons.size());
        return new UploadResult(imported, skippedReasons.size(), skippedReasons);
    }

    private void validateCsvUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileUploadException("No file was uploaded, or the file is empty.");
        }
        if (file.getSize() > MAX_CSV_BYTES) {
            throw new InvalidFileUploadException("CSV file is too large (max 5 MB).");
        }
        String raw = file.getOriginalFilename();
        String name = raw == null ? "" : raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim().toLowerCase();
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

    private static String sanitizeField(String value) {
        if (value == null) return "";
        String s = value.trim();
        if (s.length() > MAX_FIELD_LENGTH) {
            s = s.substring(0, MAX_FIELD_LENGTH);
        }
        // Strip CSV-injection prefixes that spreadsheets interpret as formulas
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = s.substring(1).trim();
        }
        // Strip control characters (keep printable ASCII + common Unicode)
        s = s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        return s;
    }

    private static Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildStats(String[] cols, int lineNum, List<String> warnings) {
        if (cols.length < 6) return "{}";

        Integer matches = cols.length > 5 ? parseIntSafe(cols[5].trim()) : null;
        Integer runs    = cols.length > 6 ? parseIntSafe(cols[6].trim()) : null;
        Integer wickets = cols.length > 7 ? parseIntSafe(cols[7].trim()) : null;

        if (matches == null && runs == null && wickets == null) return "{}";

        StringBuilder sb = new StringBuilder("{");
        if (matches != null) sb.append("\"matches\":").append(matches).append(",");
        if (runs != null)    sb.append("\"runs\":").append(runs).append(",");
        if (wickets != null) sb.append("\"wickets\":").append(wickets).append(",");
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
