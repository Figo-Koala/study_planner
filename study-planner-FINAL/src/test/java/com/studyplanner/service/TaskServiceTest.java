package com.studyplanner.service;

import com.studyplanner.exception.InvalidTaskException;
import com.studyplanner.model.AssignmentTask;
import com.studyplanner.model.ExamTask;
import com.studyplanner.model.Task;
import com.studyplanner.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskService}.
 *
 * <p>Uses an {@link InMemoryTaskRepository} as the backing store so tests
 * run fast without any I/O.</p>
 *
 * <p>Covers:
 * <ul>
 *   <li>createTask() — valid input, invalid fields, duplicate IDs</li>
 *   <li>getTask() — found, not found</li>
 *   <li>updateTask() — success, invalid, not found</li>
 *   <li>deleteTask() — success, not found</li>
 *   <li>markComplete / markIncomplete / toggleCompletion</li>
 *   <li>All required validation edge cases</li>
 * </ul>
 * </p>
 */
@DisplayName("TaskService Tests")
class TaskServiceTest {

    private TaskService service;
    private LocalDate future;
    private LocalDate past;

    @BeforeEach
    void setUp() {
        service = new TaskService(new TaskRepository(":memory:"));
        future  = LocalDate.now().plusDays(7);
        past    = LocalDate.now().minusDays(1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AssignmentTask validTask(String id) {
        return new AssignmentTask(id, "Valid Title", "CS101", future, 60, 3, 3);
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTask() succeeds for a valid task")
    void create_validTask_succeeds() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        assertEquals(1, service.getTaskCount());
    }

    @Test
    @DisplayName("createTask() throws for null task")
    void create_nullTask_throws() {
        assertThrows(InvalidTaskException.class, () -> service.createTask(null));
    }

    @Test
    @DisplayName("createTask() throws for blank title")
    void create_blankTitle_throws() {
        AssignmentTask task = new AssignmentTask("T1", "   ", "CS101", future, 60, 3, 3);
        InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                () -> service.createTask(task));
        assertEquals("title", ex.getFieldName());
    }

    @Test
    @DisplayName("createTask() throws for blank course name")
    void create_blankCourse_throws() {
        AssignmentTask task = new AssignmentTask("T1", "Title", "", future, 60, 3, 3);
        assertThrows(InvalidTaskException.class, () -> service.createTask(task));
    }

    @Test
    @DisplayName("createTask() throws for negative estimated minutes")
    void create_negativeMinutes_throws() {
        AssignmentTask task = new AssignmentTask("T1", "Title", "CS", future, -30, 3, 3);
        InvalidTaskException ex = assertThrows(InvalidTaskException.class,
                () -> service.createTask(task));
        assertEquals("estimatedMinutes", ex.getFieldName());
    }

    @Test
    @DisplayName("createTask() throws for difficulty out of range (0)")
    void create_difficultyTooLow_throws() {
        AssignmentTask task = new AssignmentTask("T1", "Title", "CS", future, 60, 0, 3);
        assertThrows(InvalidTaskException.class, () -> service.createTask(task));
    }

    @Test
    @DisplayName("createTask() throws for difficulty out of range (6)")
    void create_difficultyTooHigh_throws() {
        AssignmentTask task = new AssignmentTask("T1", "Title", "CS", future, 60, 6, 3);
        assertThrows(InvalidTaskException.class, () -> service.createTask(task));
    }

    @Test
    @DisplayName("createTask() throws for importance out of range")
    void create_importanceOutOfRange_throws() {
        AssignmentTask task = new AssignmentTask("T1", "Title", "CS", future, 60, 3, 0);
        assertThrows(InvalidTaskException.class, () -> service.createTask(task));
    }

    @Test
    @DisplayName("createTask() throws for duplicate task ID")
    void create_duplicateId_throws() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        assertThrows(InvalidTaskException.class, () -> service.createTask(validTask("T1")));
        assertEquals(1, service.getTaskCount(), "No duplicate should be stored");
    }

    @Test
    @DisplayName("createTask() accepts a past deadline (overdue tasks are valid)")
    void create_pastDeadline_isValid() {
        AssignmentTask task = new AssignmentTask("T1", "Late HW", "CS101", past, 60, 3, 3);
        assertDoesNotThrow(() -> service.createTask(task));
    }

    @Test
    @DisplayName("createTask() accepts estimatedMinutes = 0")
    void create_zeroMinutes_isValid() {
        AssignmentTask task = new AssignmentTask("T1", "Title", "CS", future, 0, 1, 1);
        assertDoesNotThrow(() -> service.createTask(task));
    }

    // ── getTask ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTask() returns the correct task")
    void getTask_found() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        Task retrieved = service.getTask("T1");
        assertEquals("T1", retrieved.getTaskId());
    }

    @Test
    @DisplayName("getTask() throws for unknown ID")
    void getTask_notFound_throws() {
        assertThrows(InvalidTaskException.class, () -> service.getTask("GHOST"));
    }

    @Test
    @DisplayName("getTask() throws for blank ID")
    void getTask_blankId_throws() {
        assertThrows(InvalidTaskException.class, () -> service.getTask("  "));
    }

    // ── getAllTasks ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllTasks() returns all added tasks")
    void getAllTasks_multipleAdded() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        service.createTask(validTask("T2"));
        service.createTask(validTask("T3"));
        List<Task> all = service.getAllTasks();
        assertEquals(3, all.size());
    }

    // ── updateTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTask() successfully changes task fields")
    void updateTask_success() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        Task t = service.getTask("T1");
        t.setTitle("Updated Title");
        service.updateTask(t);
        assertEquals("Updated Title", service.getTask("T1").getTitle());
    }

    @Test
    @DisplayName("updateTask() throws for task not in repository")
    void updateTask_notFound_throws() {
        AssignmentTask ghost = validTask("GHOST");
        assertThrows(InvalidTaskException.class, () -> service.updateTask(ghost));
    }

    @Test
    @DisplayName("updateTask() throws for invalid updated task (blank title)")
    void updateTask_invalidTitle_throws() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        Task t = service.getTask("T1");
        t.setTitle("");
        assertThrows(InvalidTaskException.class, () -> service.updateTask(t));
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTask() removes task from repository")
    void deleteTask_success() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        service.deleteTask("T1");
        assertEquals(0, service.getTaskCount());
        assertThrows(InvalidTaskException.class, () -> service.getTask("T1"));
    }

    @Test
    @DisplayName("deleteTask() throws for non-existent ID")
    void deleteTask_notFound_throws() {
        assertThrows(InvalidTaskException.class, () -> service.deleteTask("GHOST"));
    }

    // ── completion status ─────────────────────────────────────────────────────

    @Test
    @DisplayName("markComplete() sets completion status to true")
    void markComplete_success() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        assertFalse(service.getTask("T1").isCompletionStatus());
        service.markComplete("T1");
        assertTrue(service.getTask("T1").isCompletionStatus());
    }

    @Test
    @DisplayName("markIncomplete() sets completion status to false")
    void markIncomplete_success() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        service.markComplete("T1");
        service.markIncomplete("T1");
        assertFalse(service.getTask("T1").isCompletionStatus());
    }

    @Test
    @DisplayName("toggleCompletion() flips the status each call")
    void toggleCompletion_flips() throws InvalidTaskException {
        service.createTask(validTask("T1"));
        service.toggleCompletion("T1");
        assertTrue(service.getTask("T1").isCompletionStatus());
        service.toggleCompletion("T1");
        assertFalse(service.getTask("T1").isCompletionStatus());
    }

    @Test
    @DisplayName("markComplete() throws for unknown task ID")
    void markComplete_unknownId_throws() {
        assertThrows(InvalidTaskException.class, () -> service.markComplete("GHOST"));
    }

    // ── taskExists ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("taskExists() returns true/false correctly")
    void taskExists_correctResults() throws InvalidTaskException {
        assertFalse(service.taskExists("T1"));
        service.createTask(validTask("T1"));
        assertTrue(service.taskExists("T1"));
    }

    // ── constructor guard ─────────────────────────────────────────────────────

    @Test
    @DisplayName("TaskService constructor throws for null repository")
    void constructor_nullRepo_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TaskService(null));
    }
}

