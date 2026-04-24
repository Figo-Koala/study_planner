package com.studyplanner.exception;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/*
 * SchedulingConflictException - Thrown when the generated study plan has overloaded days.
 * Stores the list of conflicting dates and the daily minute limit that was exceeded.
 */
public class SchedulingConflictException extends Exception {

    private final List<LocalDate> conflictDates;
    private final int dailyLimitMinutes;

    public SchedulingConflictException(String message, List<LocalDate> conflictDates, int dailyLimit) {
        super(message);
        this.conflictDates = conflictDates != null
                ? Collections.unmodifiableList(conflictDates) : Collections.emptyList();
        this.dailyLimitMinutes = dailyLimit;
    }

    public SchedulingConflictException(String message) {
        super(message);
        this.conflictDates = Collections.emptyList();
        this.dailyLimitMinutes = 0;
    }

    // Returns dates where the daily study limit was exceeded.
    public List<LocalDate> getConflictDates()  { return conflictDates; }

    // Returns the daily minute limit that was exceeded.
    public int getDailyLimitMinutes()           { return dailyLimitMinutes; }
}
