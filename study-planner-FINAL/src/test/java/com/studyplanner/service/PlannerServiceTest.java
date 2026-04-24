package com.studyplanner.service;

import com.studyplanner.exception.InvalidTaskException;
import com.studyplanner.exception.SchedulingConflictException;
import com.studyplanner.model.*;
import com.studyplanner.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/*
 * PlannerServiceTest - Unit tests for PlannerService using an in-memory SQLite repository.
 * Covers: priority calculation, plan generation, completed-task exclusion, AI notes,
 * overload detection, daily limit setter, and constructor guard.
 */
@DisplayName("PlannerService Tests")
class PlannerServiceTest {

    private TaskRepository repo;
    private PlannerService plannerService;
    private TaskService taskService;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        repo           = new TaskRepository(":memory:");
        plannerService = new PlannerService(repo);
        taskService    = new TaskService(repo);
        today          = LocalDate.now();
    }

    // Creates an AssignmentTask with relative deadline and given params.
    private AssignmentTask task(String id, int daysUntil, int estMins, int diff, int imp) {
        return new AssignmentTask(id, "Task-" + id, "CS101",
                today.plusDays(daysUntil), estMins, diff, imp);
    }

    // ── calculatePriority ─────────────────────────────────────────────────────

    @Test
    @DisplayName("calculatePriority() matches task.calculatePriorityScore()")
    void calculatePriority_matchesTask() {
        AssignmentTask t = task("T1", 5, 120, 4, 4);
        assertEquals(t.calculatePriorityScore(), plannerService.calculatePriority(t), 0.001);
    }

    @Test
    @DisplayName("calculatePriority() returns 0 for null task")
    void calculatePriority_null_returnsZero() {
        assertEquals(0.0, plannerService.calculatePriority(null), 0.001);
    }

    // ── generateStudyPlan ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty repository produces an empty plan without error")
    void generatePlan_emptyRepo_emptyPlan() throws SchedulingConflictException {
        StudyPlan plan = plannerService.generateStudyPlan();
        assertNotNull(plan);
        assertEquals(0, plan.getTotalTaskCount());
    }

    @Test
    @DisplayName("Generated plan contains all incomplete tasks")
    void generatePlan_containsIncompleteTasks() throws InvalidTaskException, SchedulingConflictException {
        taskService.createTask(task("T1", 2, 60, 3, 3));
        taskService.createTask(task("T2", 4, 90, 2, 4));
        StudyPlan plan = plannerService.generateStudyPlan(7, false);
        assertEquals(2, plan.getTotalTaskCount());
    }

    @Test
    @DisplayName("Completed tasks are excluded from the study plan")
    void generatePlan_excludesCompleted() throws InvalidTaskException, SchedulingConflictException {
        taskService.createTask(task("T1", 3, 60, 3, 3));
        taskService.createTask(task("T2", 5, 60, 3, 3));
        taskService.markComplete("T1");
        StudyPlan plan = plannerService.generateStudyPlan(7, false);
        assertEquals(1, plan.getTotalTaskCount());
    }

    @Test
    @DisplayName("Plan with AI notes attaches at least one note (mock service)")
    void generatePlan_withAiNotes_attachesNotes() throws InvalidTaskException, SchedulingConflictException {
        plannerService.setAiService(new com.studyplanner.ai.MockAIRecommendationService());
        taskService.createTask(task("T1", 3, 60, 4, 4));
        StudyPlan plan = plannerService.generateStudyPlan(7, true);
        assertFalse(plan.getAiNotes().isEmpty());
    }

    @Test
    @DisplayName("No AI service attaches a fallback note explaining AI is unavailable")
    void generatePlan_noAiService_fallbackNote() throws InvalidTaskException, SchedulingConflictException {
        plannerService.setAiService(null);
        taskService.createTask(task("T1", 3, 60, 3, 3));
        StudyPlan plan = plannerService.generateStudyPlan(7, true);
        assertFalse(plan.getAiNotes().isEmpty());
    }

    @Test
    @DisplayName("Plan title contains the day count")
    void generatePlan_title_containsDays() throws SchedulingConflictException {
        StudyPlan plan = plannerService.generateStudyPlan(14, false);
        assertTrue(plan.getPlanTitle().contains("14"));
    }

    // ── daily limit ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("setDailyLimitMinutes() with valid value is respected")
    void dailyLimit_customValue() {
        plannerService.setDailyLimitMinutes(240);
        assertEquals(240, plannerService.getDailyLimitMinutes());
    }

    @Test
    @DisplayName("setDailyLimitMinutes(0) falls back to DEFAULT_DAILY_LIMIT")
    void dailyLimit_zero_usesDefault() {
        plannerService.setDailyLimitMinutes(0);
        assertEquals(PlannerService.DEFAULT_DAILY_LIMIT, plannerService.getDailyLimitMinutes());
    }

    // ── conflict detection ────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty plan has no overloaded days")
    void noConflict_emptyPlan() throws SchedulingConflictException {
        StudyPlan plan = plannerService.generateStudyPlan();
        assertTrue(plan.getOverloadedDays().isEmpty());
    }

    @Test
    @DisplayName("Many large tasks on one day trigger SchedulingConflictException")
    void generatePlan_overloaded_throwsConflict() throws InvalidTaskException {
        // 10 tasks × 180 min = 1800 min on 1 day — exceeds 480 min limit
        for (int i = 1; i <= 10; i++) {
            taskService.createTask(task("T" + i, 1, 180, 5, 5));
        }
        assertThrows(SchedulingConflictException.class,
                () -> plannerService.generateStudyPlan(1, false));
    }

    // ── constructor guard ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PlannerService constructor throws for null repository")
    void constructor_nullRepo_throws() {
        assertThrows(IllegalArgumentException.class, () -> new PlannerService(null));
    }
}
