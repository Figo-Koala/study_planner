package com.studyplanner.model;

import java.time.LocalDate;

/*
 * AssignmentTask - Represents a homework assignment, project, or essay.
 * Extends Task with: submissionType (e.g. "PDF", "GitHub") and hasDeliverable flag.
 * Overrides calculatePriorityScore() to add +5 when a deliverable is required.
 */
public class AssignmentTask extends Task {

    private String submissionType;
    private boolean hasDeliverable;

    public AssignmentTask(String taskId, String title, String courseName,
                          LocalDate deadline, int estimatedMinutes,
                          int difficulty, int importance,
                          String submissionType, boolean hasDeliverable) {
        super(taskId, title, courseName, deadline, estimatedMinutes, difficulty, importance);
        this.submissionType = submissionType;
        this.hasDeliverable = hasDeliverable;
    }

    // Convenience constructor with default submission type and no deliverable.
    public AssignmentTask(String taskId, String title, String courseName,
                          LocalDate deadline, int estimatedMinutes,
                          int difficulty, int importance) {
        this(taskId, title, courseName, deadline, estimatedMinutes,
             difficulty, importance, "Standard", false);
    }

    @Override
    public String getTaskType() { return "ASSIGNMENT"; }

    @Override
    public String getTypeSpecificDetails() {
        return "Submission: " + submissionType + " | Deliverable: " + (hasDeliverable ? "Yes" : "No");
    }

    // Adds +5 to base priority if a deliverable artifact is required.
    @Override
    public double calculatePriorityScore() {
        double base = super.calculatePriorityScore();
        return hasDeliverable ? Math.min(100.0, base + 5.0) : base;
    }

    public String  getSubmissionType()                       { return submissionType; }
    public void    setSubmissionType(String submissionType)  { this.submissionType = submissionType; }
    public boolean isHasDeliverable()                        { return hasDeliverable; }
    public void    setHasDeliverable(boolean hasDeliverable) { this.hasDeliverable = hasDeliverable; }

    @Override
    public String toString() {
        return super.toString() + " | " + getTypeSpecificDetails();
    }
}
