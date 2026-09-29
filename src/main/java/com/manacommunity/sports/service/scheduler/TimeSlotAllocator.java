package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Owns time-slot allocation for generated schedules: where the day starts and
 * how successive matches are spaced (duration + break). Extracted from the
 * inline cursors that used to live in every generator.
 */
@Service
public class TimeSlotAllocator {

    /** Default start-of-play time for a generated day (09:00). */
    public LocalDateTime dayStart(LocalDate date) {
        return date.atTime(9, 0);
    }

    /** A sequential slot cursor spaced by (durationMins + breakMins). */
    public Cursor cursor(LocalDate startDate, int durationMins, int breakMins) {
        return new Cursor(dayStart(startDate), durationMins + breakMins);
    }

    /** A sequential slot cursor from an explicit start time. */
    public Cursor cursor(LocalDateTime start, int durationMins, int breakMins) {
        return new Cursor(start, durationMins + breakMins);
    }

    /** Parse a UI-supplied date/time ("yyyy-MM-dd" + "HH:mm") into a timestamp. */
    public LocalDateTime parseScheduledAt(String date, String time) {
        try {
            String t = (time != null && !time.isBlank()) ? time : "00:00";
            return LocalDateTime.parse(date + "T" + t);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    /**
     * Mutable cursor that yields successive, evenly-spaced match start times.
     * {@link #next()} returns the current slot and advances; {@link #peek()}
     * and {@link #set(LocalDateTime)} allow callers that need to jump.
     */
    public static final class Cursor {
        private LocalDateTime current;
        private final int stepMinutes;

        Cursor(LocalDateTime start, int stepMinutes) {
            this.current = start;
            this.stepMinutes = stepMinutes;
        }

        public LocalDateTime next() {
            LocalDateTime slot = current;
            current = current.plusMinutes(stepMinutes);
            return slot;
        }

        public LocalDateTime peek() {
            return current;
        }

        public void set(LocalDateTime at) {
            this.current = at;
        }
    }
}

