package com.studyplanner.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/*
 * Task - Abstract base class for all study task types.
 * Stores common fields: taskId, title, courseName, deadline, estimatedMinutes,
 * difficulty (1-5), importance (1-5), and completionStatus.
 * Subclasses add type-specific fields and override calculatePriorityScore().
 */
public abstract class Task {

    private String taskId;
    private String title;
    private String courseName;
    private LocalDate deadline;
    private int estimatedMinutes;
    private int difficulty;
    private int importance;
    private boolean completionStatus;

    protected Task(String taskId, String title, String courseName,
                   LocalDate deadline, int estimatedMinutes,
                   int difficulty, int importance) {
        this.taskId = taskId;
        this.title = title;
        this.courseName = courseName;
        this.deadline = deadline;
        this.estimatedMinutes = estimatedMinutes;
        this.difficulty = difficulty;
        this.importance = importance;
        this.completionStatus = false;
    }

    // Returns the type label: "ASSIGNMENT", "EXAM", or "READING".
    public abstract String getTaskType();

    // Returns a short string describing subtype-specific fields.
    public abstract String getTypeSpecificDetails();

    /*
     * Calculates a priority score from 0 to 100.
     * Weights: urgency 40%, difficulty 30%, importance 20%, duration 10%.
     * Urgency is 100 when overdue, 0 when 30+ days away.
     * Subclasses may override to add their own adjustments.
     */
    public double calculatePriorityScore() {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), deadline);

        double urgency;
        if (daysUntil <= 0)      urgency = 100.0;
        else if (daysUntil >= 30) urgency = 0.0;
        else                      urgency = (30.0 - daysUntil) / 30.0 * 100.0;

        double diffScore = difficulty * 20.0;
        double impScore  = importance * 20.0;
        double durScore  = Math.min(100.0, (estimatedMinutes / 120.0) * 100.0);

        return urgency * 0.40 + diffScore * 0.30 + impScore * 0.20 + durScore * 0.10;
    }

    public String  getTaskId()                           { return taskId; }
    public void    setTaskId(String taskId)              { this.taskId = taskId; }
    public String  getTitle()                            { return title; }
    public void    setTitle(String title)                { this.title = title; }
    public String  getCourseName()                       { return courseName; }
    public void    setCourseName(String courseName)      { this.courseName = courseName; }
    public LocalDate getDeadline()                       { return deadline; }
    public void    setDeadline(LocalDate deadline)       { this.deadline = deadline; }
    public int     getEstimatedMinutes()                 { return estimatedMinutes; }
    public void    setEstimatedMinutes(int v)            { this.estimatedMinutes = v; }
    public int     getDifficulty()                       { return difficulty; }
    public void    setDifficulty(int difficulty)         { this.difficulty = difficulty; }
    public int     getImportance()                       { return importance; }
    public void    setImportance(int importance)         { this.importance = importance; }
    public boolean isCompletionStatus()                  { return completionStatus; }
    public void    setCompletionStatus(boolean status)   { this.completionStatus = status; }

    @Override
    public String toString() {
        String status = completionStatus ? "DONE" : "TODO";
        return String.format("[%s][%s] %s | %s | Due: %s | Est: %dmin | D:%d I:%d | Pri:%.1f",
                status, getTaskType(), title, courseName, deadline,
                estimatedMinutes, difficulty, importance, calculatePriorityScore());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        return taskId != null && taskId.equals(((Task) o).taskId);
    }

    @Override
    public int hashCode() {
        return taskId != null ? taskId.hashCode() : 0;
    }
}
