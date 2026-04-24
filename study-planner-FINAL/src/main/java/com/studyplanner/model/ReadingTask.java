package com.studyplanner.model;

import java.time.LocalDate;

/*
 * ReadingTask - Represents a reading or review task (textbook, notes, articles).
 * Extends Task with: pageCount.
 * Overrides calculatePriorityScore() to add a small bonus for long readings.
 */
public class ReadingTask extends Task {

    private int pageCount;

    public ReadingTask(String taskId, String title, String courseName,
                       LocalDate deadline, int estimatedMinutes,
                       int difficulty, int importance, int pageCount) {
        super(taskId, title, courseName, deadline, estimatedMinutes, difficulty, importance);
        this.pageCount = Math.max(0, pageCount);
    }

    // Convenience constructor with pageCount = 0 (unspecified).
    public ReadingTask(String taskId, String title, String courseName,
                       LocalDate deadline, int estimatedMinutes,
                       int difficulty, int importance) {
        this(taskId, title, courseName, deadline, estimatedMinutes, difficulty, importance, 0);
    }

    @Override
    public String getTaskType() { return "READING"; }

    @Override
    public String getTypeSpecificDetails() {
        return "Pages: " + (pageCount > 0 ? pageCount : "unspecified");
    }

    // Adds up to +5 for readings over 50 pages (every 50 extra pages = +1).
    @Override
    public double calculatePriorityScore() {
        double base = super.calculatePriorityScore();
        if (pageCount > 50) {
            base = Math.min(100.0, base + Math.min(5.0, (pageCount - 50) / 50.0));
        }
        return base;
    }

    public int  getPageCount()              { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = Math.max(0, pageCount); }

    @Override
    public String toString() {
        return super.toString() + " | " + getTypeSpecificDetails();
    }
}
