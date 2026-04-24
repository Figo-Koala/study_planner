package com.studyplanner.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Task class hierarchy.
 *
 * <p>Covers:
 * <ul>
 *   <li>Correct construction and field storage for all three subtypes</li>
 *   <li>Subtype-specific fields (submissionType, topicList, pageCount)</li>
 *   <li>Priority score calculation — including subtype overrides</li>
 *   <li>Completion status toggling</li>
 *   <li>Polymorphism — getTaskType() returns the correct discriminator</li>
 * </ul>
 * </p>
 */
@DisplayName("Task Hierarchy Tests")
class TaskTest {

    // ── Shared fixtures ────────────────────────────────────────────────────────

    private LocalDate futureDate;
    private LocalDate pastDate;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today      = LocalDate.now();
        futureDate = today.plusDays(10);
        pastDate   = today.minusDays(5);
    }

    // ── AssignmentTask ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("AssignmentTask stores all fields correctly")
    void assignmentTask_storesFields() {
        AssignmentTask task = new AssignmentTask(
                "A1", "Java HW", "CS101", futureDate, 120, 3, 4,
                "GitHub", true);

        assertEquals("A1",         task.getTaskId());
        assertEquals("Java HW",    task.getTitle());
        assertEquals("CS101",      task.getCourseName());
        assertEquals(futureDate,   task.getDeadline());
        assertEquals(120,          task.getEstimatedMinutes());
        assertEquals(3,            task.getDifficulty());
        assertEquals(4,            task.getImportance());
        assertEquals("GitHub",     task.getSubmissionType());
        assertTrue(task.isHasDeliverable());
        assertFalse(task.isCompletionStatus(), "New task must start as incomplete");
    }

    @Test
    @DisplayName("AssignmentTask.getTaskType() returns ASSIGNMENT")
    void assignmentTask_taskType() {
        AssignmentTask task = new AssignmentTask(
                "A1", "HW", "CS", futureDate, 60, 2, 3);
        assertEquals("ASSIGNMENT", task.getTaskType());
    }

    @Test
    @DisplayName("AssignmentTask priority boost when hasDeliverable=true")
    void assignmentTask_deliverableBoostsPriority() {
        // Same task, one with deliverable and one without
        AssignmentTask withDel    = new AssignmentTask("A1", "T", "C", today, 60, 3, 3, "PDF", true);
        AssignmentTask withoutDel = new AssignmentTask("A2", "T", "C", today, 60, 3, 3, "PDF", false);

        // With deliverable should score higher
        assertTrue(withDel.calculatePriorityScore() > withoutDel.calculatePriorityScore(),
                "Deliverable task should have higher priority score");
    }

    @Test
    @DisplayName("AssignmentTask convenience constructor defaults")
    void assignmentTask_convenienceConstructor() {
        AssignmentTask task = new AssignmentTask("A1", "HW", "CS", futureDate, 60, 2, 3);
        assertEquals("Standard", task.getSubmissionType());
        assertFalse(task.isHasDeliverable());
    }

    // ── ExamTask ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("ExamTask stores topics and isCumulative flag")
    void examTask_storesFields() {
        ExamTask exam = new ExamTask("E1", "Midterm", "MATH201", futureDate, 180, 5, 5,
                Arrays.asList("Calculus", "Algebra"), true);

        assertEquals("E1",         exam.getTaskId());
        assertEquals("EXAM",       exam.getTaskType());
        assertEquals(2,            exam.getTopicList().size());
        assertTrue(exam.getTopicList().contains("Calculus"));
        assertTrue(exam.isCumulative());
    }

    @Test
    @DisplayName("ExamTask.addTopic and removeTopic work correctly")
    void examTask_topicManagement() {
        ExamTask exam = new ExamTask("E1", "Final", "CS101", futureDate, 120, 4, 5);
        exam.addTopic("Sorting");
        exam.addTopic("Graphs");
        exam.addTopic(""); // blank — should be ignored
        exam.addTopic(null); // null — should be ignored

        assertEquals(2, exam.getTopicList().size());
        exam.removeTopic("Sorting");
        assertEquals(1, exam.getTopicList().size());
        assertTrue(exam.getTopicList().contains("Graphs"));
    }

    @Test
    @DisplayName("ExamTask cumulative flag adds priority bonus")
    void examTask_cumulativeBoostsPriority() {
        ExamTask cumulative    = new ExamTask("E1", "Final", "C", today, 120, 4, 4, null, true);
        ExamTask nonCumulative = new ExamTask("E2", "Final", "C", today, 120, 4, 4, null, false);

        assertTrue(cumulative.calculatePriorityScore() > nonCumulative.calculatePriorityScore(),
                "Cumulative exam should score higher");
    }

    @Test
    @DisplayName("ExamTask topicList is a defensive copy (internal list is independent)")
    void examTask_defensiveCopy() {
        java.util.List<String> original = new java.util.ArrayList<>(Arrays.asList("A", "B"));
        ExamTask exam = new ExamTask("E1", "Quiz", "C", futureDate, 60, 2, 2, original, false);
        original.add("C"); // mutate the original list
        assertEquals(2, exam.getTopicList().size(), "Internal list should not be affected");
    }

    // ── ReadingTask ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("ReadingTask stores pageCount correctly")
    void readingTask_storesFields() {
        ReadingTask rt = new ReadingTask("R1", "Chapter 3", "LIT201", futureDate, 90, 2, 3, 45);

        assertEquals("R1",       rt.getTaskId());
        assertEquals("READING",  rt.getTaskType());
        assertEquals(45,         rt.getPageCount());
    }

    @Test
    @DisplayName("ReadingTask negative page count clamped to 0")
    void readingTask_negativePageCountClamped() {
        ReadingTask rt = new ReadingTask("R1", "T", "C", futureDate, 60, 2, 2, -10);
        assertEquals(0, rt.getPageCount(), "Negative pages should be clamped to 0");
    }

    @Test
    @DisplayName("ReadingTask high page count adds small priority bonus")
    void readingTask_highPageCountBonusPriority() {
        ReadingTask shortRead = new ReadingTask("R1", "T", "C", futureDate, 60, 3, 3, 10);
        ReadingTask longRead  = new ReadingTask("R2", "T", "C", futureDate, 60, 3, 3, 200);

        assertTrue(longRead.calculatePriorityScore() >= shortRead.calculatePriorityScore(),
                "Longer reading should score >= shorter reading");
    }

    // ── Priority score correctness ────────────────────────────────────────────

    @Test
    @DisplayName("Priority score is in [0, 100]")
    void priorityScore_withinBounds() {
        AssignmentTask task = new AssignmentTask("A1", "T", "C", today.plusDays(1), 60, 5, 5);
        double score = task.calculatePriorityScore();
        assertTrue(score >= 0.0 && score <= 100.0,
                "Priority must be in [0, 100] but was: " + score);
    }

    @Test
    @DisplayName("Overdue task has maximum urgency (priority >= 40)")
    void priorityScore_overdueMaxUrgency() {
        AssignmentTask task = new AssignmentTask("A1", "T", "C", pastDate, 60, 1, 1);
        double score = task.calculatePriorityScore();
        // Urgency component alone contributes 40 for overdue tasks
        assertTrue(score >= 40.0, "Overdue task priority should be at least 40, was: " + score);
    }

    @Test
    @DisplayName("Far-future task has lower priority than near-deadline task")
    void priorityScore_nearDeadlineHigherThanFar() {
        AssignmentTask nearTask = new AssignmentTask("A1", "T", "C", today.plusDays(1), 60, 3, 3);
        AssignmentTask farTask  = new AssignmentTask("A2", "T", "C", today.plusDays(60), 60, 3, 3);

        assertTrue(nearTask.calculatePriorityScore() > farTask.calculatePriorityScore(),
                "Closer deadline should yield higher priority");
    }

    // ── Completion status ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Completion status starts false, can be toggled")
    void completionStatus_toggles() {
        AssignmentTask task = new AssignmentTask("A1", "T", "C", futureDate, 60, 2, 2);
        assertFalse(task.isCompletionStatus());

        task.setCompletionStatus(true);
        assertTrue(task.isCompletionStatus());

        task.setCompletionStatus(false);
        assertFalse(task.isCompletionStatus());
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Two tasks with the same ID are equal")
    void equals_sameId() {
        AssignmentTask t1 = new AssignmentTask("A1", "HW", "CS", futureDate, 60, 2, 2);
        AssignmentTask t2 = new AssignmentTask("A1", "Different", "Different", futureDate, 90, 4, 4);
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    @DisplayName("Two tasks with different IDs are not equal")
    void equals_differentId() {
        AssignmentTask t1 = new AssignmentTask("A1", "HW", "CS", futureDate, 60, 2, 2);
        AssignmentTask t2 = new AssignmentTask("A2", "HW", "CS", futureDate, 60, 2, 2);
        assertNotEquals(t1, t2);
    }
}
