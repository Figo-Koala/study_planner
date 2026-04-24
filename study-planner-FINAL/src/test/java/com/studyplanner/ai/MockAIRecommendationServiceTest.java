package com.studyplanner.ai;

import com.studyplanner.model.AssignmentTask;
import com.studyplanner.model.ExamTask;
import com.studyplanner.model.ReadingTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/*
 * MockAIRecommendationServiceTest - Tests the offline mock AI service.
 * Verifies: priority explanation (non-null, length limit, urgency for overdue tasks),
 * breakdown per task type, null handling, determinism, and mode identification.
 */
@DisplayName("MockAIRecommendationService Tests")
class MockAIRecommendationServiceTest {

    private MockAIRecommendationService mockAI;
    private LocalDate future;
    private LocalDate past;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        mockAI = new MockAIRecommendationService();
        today  = LocalDate.now();
        future = today.plusDays(5);
        past   = today.minusDays(3);
    }

    // ── Mode identification ────────────────────────────────────────────────────

    @Test
    @DisplayName("getModeName() returns a non-blank string")
    void getModeName_nonBlank() {
        assertNotNull(mockAI.getModeName());
        assertFalse(mockAI.getModeName().isBlank());
    }

    @Test
    @DisplayName("isOnlineMode() returns false for mock")
    void isOnlineMode_false() {
        assertFalse(mockAI.isOnlineMode());
    }

    // ── generatePriorityExplanation ───────────────────────────────────────────

    @Test
    @DisplayName("Priority explanation for a valid task is non-null and non-blank")
    void priorityExplanation_validTask_nonNull() {
        AssignmentTask task = new AssignmentTask("T1", "HW", "CS101", future, 60, 3, 4);
        String result = mockAI.generatePriorityExplanation(task);
        assertNotNull(result);
        assertFalse(result.isBlank(), "Explanation must not be blank");
    }

    @Test
    @DisplayName("Priority explanation for null task returns fallback message")
    void priorityExplanation_nullTask_fallback() {
        String result = mockAI.generatePriorityExplanation(null);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Priority explanation for overdue task mentions urgency")
    void priorityExplanation_overdueTask_mentionsOverdue() {
        AssignmentTask task = new AssignmentTask("T1", "Late HW", "CS", past, 60, 3, 3);
        String result = mockAI.generatePriorityExplanation(task);
        // Should contain some indication of urgency or being overdue
        assertTrue(result.toLowerCase().contains("overdue") || result.toLowerCase().contains("immediately"),
                "Explanation for overdue task should mention urgency. Got: " + result);
    }

    @Test
    @DisplayName("Priority explanation is <= 500 characters")
    void priorityExplanation_lengthLimited() {
        AssignmentTask task = new AssignmentTask("T1", "HW", "CS", future, 60, 5, 5);
        String result = mockAI.generatePriorityExplanation(task);
        assertTrue(result.length() <= 500,
                "Explanation should be at most 500 chars, was: " + result.length());
    }

    @Test
    @DisplayName("Priority explanation contains the priority score")
    void priorityExplanation_containsScore() {
        AssignmentTask task = new AssignmentTask("T1", "HW", "CS", future, 60, 3, 3);
        String result = mockAI.generatePriorityExplanation(task);
        // Should mention the numeric score or a priority label
        boolean hasPriorityInfo = result.contains("Priority") || result.contains("score")
                || result.contains("HIGH") || result.contains("MEDIUM")
                || result.contains("LOW") || result.contains("CRITICAL");
        assertTrue(hasPriorityInfo,
                "Explanation should mention priority information. Got: " + result);
    }

    // ── generateBreakdown ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Breakdown for AssignmentTask is non-null and non-blank")
    void breakdown_assignment_nonNull() {
        AssignmentTask task = new AssignmentTask("T1", "Essay", "ENG101", future, 90, 3, 4,
                "PDF", true);
        String result = mockAI.generateBreakdown(task);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Breakdown for ExamTask is non-null and non-blank")
    void breakdown_exam_nonNull() {
        ExamTask task = new ExamTask("E1", "Midterm", "MATH", future, 180, 5, 5,
                Arrays.asList("Calculus", "Algebra"), true);
        String result = mockAI.generateBreakdown(task);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Exam breakdown mentions topics when they are present")
    void breakdown_exam_mentionsTopics() {
        ExamTask task = new ExamTask("E1", "Final", "CS101", future, 180, 5, 5,
                Arrays.asList("Recursion", "Sorting"), false);
        String result = mockAI.generateBreakdown(task);
        // Topics should be referenced
        assertTrue(result.contains("Recursion") || result.contains("Sorting"),
                "Breakdown should reference exam topics. Got: " + result);
    }

    @Test
    @DisplayName("Breakdown for ReadingTask is non-null and non-blank")
    void breakdown_reading_nonNull() {
        ReadingTask task = new ReadingTask("R1", "Chapter 3", "LIT", future, 60, 2, 2, 50);
        String result = mockAI.generateBreakdown(task);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Reading breakdown for high page count mentions pages per session")
    void breakdown_reading_highPageCount_mentionsSessions() {
        ReadingTask task = new ReadingTask("R1", "Textbook", "BIO", future, 180, 3, 3, 200);
        String result = mockAI.generateBreakdown(task);
        // Should hint at breaking it into sessions
        assertTrue(result.contains("session") || result.contains("pages"),
                "Breakdown should mention sessions or pages. Got: " + result);
    }

    @Test
    @DisplayName("Breakdown for null task returns fallback message")
    void breakdown_nullTask_fallback() {
        String result = mockAI.generateBreakdown(null);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Breakdown is <= 500 characters")
    void breakdown_lengthLimited() {
        AssignmentTask task = new AssignmentTask("T1", "Project", "CS", future, 300, 5, 5);
        String result = mockAI.generateBreakdown(task);
        assertTrue(result.length() <= 500,
                "Breakdown should be at most 500 chars, was: " + result.length());
    }

    // ── Multiple calls are consistent (deterministic behavior) ────────────────

    @Test
    @DisplayName("Same input produces same output (deterministic mock)")
    void deterministic_sameInputSameOutput() {
        AssignmentTask task = new AssignmentTask("T1", "HW", "CS", future, 60, 3, 3);
        String result1 = mockAI.generatePriorityExplanation(task);
        String result2 = mockAI.generatePriorityExplanation(task);
        assertEquals(result1, result2, "Mock AI should be deterministic");
    }
}
