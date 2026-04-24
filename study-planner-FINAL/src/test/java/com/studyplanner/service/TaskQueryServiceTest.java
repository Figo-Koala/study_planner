package com.studyplanner.service;

import com.studyplanner.exception.InvalidTaskException;
import com.studyplanner.model.AssignmentTask;
import com.studyplanner.model.ExamTask;
import com.studyplanner.model.ReadingTask;
import com.studyplanner.model.Task;
import com.studyplanner.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskQueryService}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Sorting by deadline and by priority</li>
 *   <li>Filtering by course, completion status, and task type</li>
 *   <li>Grouping by course and task type</li>
 *   <li>Top-N priority tasks</li>
 *   <li>Statistics: distinct courses, counts</li>
 *   <li>Edge cases: empty repository, unknown course filter</li>
 * </ul>
 * </p>
 */
@DisplayName("TaskQueryService Tests")
class TaskQueryServiceTest {

    private TaskQueryService queryService;
    private TaskService taskService;
    private LocalDate today;

    @BeforeEach
    void setUp() throws InvalidTaskException {
        TaskRepository repo = new TaskRepository(":memory:");
        taskService  = new TaskService(repo);
        queryService = new TaskQueryService(repo);
        today = LocalDate.now();

        // Pre-populate with 5 mixed tasks
        taskService.createTask(new AssignmentTask("A1", "Essay",       "ENG101", today.plusDays(3),  60, 2, 3));
        taskService.createTask(new AssignmentTask("A2", "Lab Report",  "BIO201", today.plusDays(10), 120, 3, 4));
        taskService.createTask(new ExamTask(      "E1", "Midterm",     "CS101",  today.plusDays(1),  180, 5, 5));
        taskService.createTask(new ReadingTask(   "R1", "Chapter 5",   "ENG101", today.plusDays(7),  90, 2, 2, 40));
        taskService.createTask(new AssignmentTask("A3", "Project",     "CS101",  today.plusDays(5),  240, 4, 4));

        // Mark one task complete
        taskService.markComplete("R1");
    }

    // ── sorting ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sortByDeadline() returns tasks in ascending deadline order")
    void sortByDeadline_ascending() {
        List<Task> sorted = queryService.getAllTasksSortedByDeadline();
        assertEquals(5, sorted.size());
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertFalse(sorted.get(i).getDeadline().isAfter(sorted.get(i + 1).getDeadline()),
                    "Deadlines should be non-decreasing");
        }
    }

    @Test
    @DisplayName("sortByPriority() returns tasks in descending priority order")
    void sortByPriority_descending() {
        List<Task> sorted = queryService.getAllTasksSortedByPriority();
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).calculatePriorityScore()
                    >= sorted.get(i + 1).calculatePriorityScore(),
                    "Priority scores should be non-increasing");
        }
    }

    @Test
    @DisplayName("Exam with closest deadline appears first in deadline sort")
    void sortByDeadline_examFirst() {
        List<Task> sorted = queryService.getAllTasksSortedByDeadline();
        assertEquals("E1", sorted.get(0).getTaskId(),
                "Midterm (due in 1 day) should be first");
    }

    // ── filtering ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("filterByCourseName() returns only tasks for that course")
    void filterByCourse_correctTasks() {
        List<Task> cs101 = queryService.filterByCourseName("CS101");
        assertEquals(2, cs101.size());
        assertTrue(cs101.stream().allMatch(t -> t.getCourseName().equals("CS101")));
    }

    @Test
    @DisplayName("filterByCourseName() is case-insensitive")
    void filterByCourse_caseInsensitive() {
        List<Task> result = queryService.filterByCourseName("cs101");
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("filterByCourseName() returns empty list for unknown course")
    void filterByCourse_unknownCourse_empty() {
        List<Task> result = queryService.filterByCourseName("PHYSICS999");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("filterByCourseName() returns empty list for null/blank input")
    void filterByCourse_blank_empty() {
        assertTrue(queryService.filterByCourseName(null).isEmpty());
        assertTrue(queryService.filterByCourseName("").isEmpty());
    }

    @Test
    @DisplayName("filterByCompletionStatus(true) returns only completed tasks")
    void filterByStatus_completed() {
        List<Task> done = queryService.filterByCompletionStatus(true);
        assertEquals(1, done.size());
        assertEquals("R1", done.get(0).getTaskId());
    }

    @Test
    @DisplayName("filterByCompletionStatus(false) returns only pending tasks")
    void filterByStatus_pending() {
        List<Task> pending = queryService.filterByCompletionStatus(false);
        assertEquals(4, pending.size());
        assertTrue(pending.stream().noneMatch(Task::isCompletionStatus));
    }

    @Test
    @DisplayName("filterByTaskType('EXAM') returns only exam tasks")
    void filterByType_exam() {
        List<Task> exams = queryService.filterByTaskType("EXAM");
        assertEquals(1, exams.size());
        assertEquals("E1", exams.get(0).getTaskId());
    }

    @Test
    @DisplayName("filterByTaskType() is case-insensitive")
    void filterByType_caseInsensitive() {
        List<Task> result = queryService.filterByTaskType("reading");
        assertEquals(1, result.size());
    }

    // ── grouping ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("groupByCourse() produces correct number of groups")
    void groupByCourse_correctGroups() {
        Map<String, List<Task>> groups = queryService.groupByCourse();
        // CS101: E1, A3 | ENG101: A1, R1 | BIO201: A2
        assertEquals(3, groups.size());
        assertEquals(2, groups.get("CS101").size());
        assertEquals(2, groups.get("ENG101").size());
        assertEquals(1, groups.get("BIO201").size());
    }

    @Test
    @DisplayName("groupByTaskType() produces correct groups")
    void groupByTaskType_correctGroups() {
        Map<String, List<Task>> groups = queryService.groupByTaskType();
        assertEquals(3, groups.size());  // ASSIGNMENT, EXAM, READING
        assertEquals(3, groups.get("ASSIGNMENT").size());
        assertEquals(1, groups.get("EXAM").size());
        assertEquals(1, groups.get("READING").size());
    }

    // ── top priority ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTopPriorityTasks(3) returns at most 3 pending tasks in priority order")
    void topPriority_returnsPendingOnly() {
        List<Task> top = queryService.getTopPriorityTasks(3);
        assertTrue(top.size() <= 3);
        // All returned tasks must be incomplete
        assertTrue(top.stream().noneMatch(Task::isCompletionStatus),
                "Top priority list must contain only pending tasks");
        // Scores must be non-increasing
        for (int i = 0; i < top.size() - 1; i++) {
            assertTrue(top.get(i).calculatePriorityScore() >= top.get(i + 1).calculatePriorityScore());
        }
    }

    @Test
    @DisplayName("getTopPriorityTasks(0) returns empty list")
    void topPriority_zeroN_empty() {
        assertTrue(queryService.getTopPriorityTasks(0).isEmpty());
    }

    // ── statistics ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("countPendingTasks() returns correct count")
    void countPending() {
        assertEquals(4, queryService.countPendingTasks());
    }

    @Test
    @DisplayName("countCompletedTasks() returns correct count")
    void countCompleted() {
        assertEquals(1, queryService.countCompletedTasks());
    }

    @Test
    @DisplayName("getDistinctCourseNames() returns sorted unique course names")
    void distinctCourses_sortedAndUnique() {
        List<String> courses = queryService.getDistinctCourseNames();
        assertEquals(3, courses.size());
        assertEquals(List.of("BIO201", "CS101", "ENG101"), courses);
    }

    // ── empty repo edge case ──────────────────────────────────────────────────

    @Test
    @DisplayName("All query methods return empty results on an empty repository")
    void emptyRepo_allMethodsReturnEmpty() {
        TaskQueryService emptyQ = new TaskQueryService(new TaskRepository(":memory:"));
        assertTrue(emptyQ.getAllTasks().isEmpty());
        assertTrue(emptyQ.getAllTasksSortedByDeadline().isEmpty());
        assertTrue(emptyQ.getAllTasksSortedByPriority().isEmpty());
        assertTrue(emptyQ.filterByCourseName("CS101").isEmpty());
        assertTrue(emptyQ.filterByCompletionStatus(false).isEmpty());
        assertTrue(emptyQ.groupByCourse().isEmpty());
        assertTrue(emptyQ.groupByTaskType().isEmpty());
        assertEquals(0, emptyQ.countPendingTasks());
        assertEquals(0, emptyQ.countCompletedTasks());
    }

    // ── constructor guard ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TaskQueryService constructor throws for null repository")
    void constructor_nullRepo_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new TaskQueryService(null));
    }
}
