package com.studyplanner.repository;

import com.studyplanner.model.AssignmentTask;
import com.studyplanner.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * TaskRepositoryTest - Unit tests for TaskRepository using an in-memory SQLite database.
 * Uses ":memory:" as the database path so no file is created and each test starts fresh.
 * Covers: save, findById, findAll, update, delete, exists, count, clear, and edge cases.
 */
@DisplayName("TaskRepository Tests")
class InMemoryTaskRepositoryTest {

    private TaskRepository repo;
    private LocalDate future;

    @BeforeEach
    void setUp() {
        repo   = new TaskRepository(":memory:");
        future = LocalDate.now().plusDays(7);
    }

    // Creates a minimal valid AssignmentTask with the given ID.
    private AssignmentTask makeTask(String id) {
        return new AssignmentTask(id, "Title-" + id, "Course", future, 60, 3, 3);
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() returns true and task becomes retrievable")
    void save_success() {
        assertTrue(repo.save(makeTask("T1")));
        assertTrue(repo.exists("T1"));
        assertEquals(1, repo.count());
    }

    @Test
    @DisplayName("save() returns false for duplicate task ID")
    void save_duplicateId_returnsFalse() {
        assertTrue(repo.save(makeTask("T1")));
        assertFalse(repo.save(makeTask("T1")));
        assertEquals(1, repo.count());
    }

    @Test
    @DisplayName("save() returns false for null task")
    void save_null_returnsFalse() {
        assertFalse(repo.save(null));
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById() returns the correct task")
    void findById_found() {
        repo.save(makeTask("T2"));
        Task found = repo.findById("T2");
        assertNotNull(found);
        assertEquals("T2", found.getTaskId());
    }

    @Test
    @DisplayName("findById() returns null for unknown ID")
    void findById_notFound() {
        assertNull(repo.findById("NONEXISTENT"));
    }

    @Test
    @DisplayName("findById() returns null for null ID")
    void findById_null() {
        assertNull(repo.findById(null));
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll() returns all saved tasks")
    void findAll_returnsAll() {
        repo.save(makeTask("T1"));
        repo.save(makeTask("T2"));
        repo.save(makeTask("T3"));
        assertEquals(3, repo.findAll().size());
    }

    @Test
    @DisplayName("findAll() returns empty list when repository is empty")
    void findAll_empty() {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    @DisplayName("findAll() returns a snapshot — mutations do not affect repo")
    void findAll_isSnapshot() {
        repo.save(makeTask("T1"));
        List<Task> list = repo.findAll();
        list.clear();
        assertEquals(1, repo.count());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update() changes the stored task and returns true")
    void update_success() {
        AssignmentTask task = makeTask("T1");
        repo.save(task);
        task.setTitle("Updated Title");
        assertTrue(repo.update(task));
        assertEquals("Updated Title", repo.findById("T1").getTitle());
    }

    @Test
    @DisplayName("update() returns false for task that does not exist")
    void update_notExist_returnsFalse() {
        assertFalse(repo.update(makeTask("GHOST")));
    }

    @Test
    @DisplayName("update() returns false for null task")
    void update_null_returnsFalse() {
        assertFalse(repo.update(null));
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() removes the task and returns true")
    void delete_success() {
        repo.save(makeTask("T1"));
        assertTrue(repo.delete("T1"));
        assertFalse(repo.exists("T1"));
        assertEquals(0, repo.count());
    }

    @Test
    @DisplayName("delete() returns false for unknown ID")
    void delete_unknownId() {
        assertFalse(repo.delete("GHOST"));
    }

    @Test
    @DisplayName("delete() returns false for null ID")
    void delete_null() {
        assertFalse(repo.delete(null));
    }

    // ── exists ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("exists() returns true for a saved task")
    void exists_true() {
        repo.save(makeTask("T1"));
        assertTrue(repo.exists("T1"));
    }

    @Test
    @DisplayName("exists() returns false for missing task")
    void exists_false() {
        assertFalse(repo.exists("MISSING"));
    }

    @Test
    @DisplayName("exists() returns false for null ID")
    void exists_null() {
        assertFalse(repo.exists(null));
    }

    // ── count ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("count() reflects saves and deletes correctly")
    void count_incrementsAndDecrements() {
        assertEquals(0, repo.count());
        repo.save(makeTask("T1"));
        repo.save(makeTask("T2"));
        assertEquals(2, repo.count());
        repo.delete("T1");
        assertEquals(1, repo.count());
    }

    // ── clear ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("clear() removes all tasks")
    void clear_removesAll() {
        repo.save(makeTask("T1"));
        repo.save(makeTask("T2"));
        repo.clear();
        assertEquals(0, repo.count());
        assertTrue(repo.findAll().isEmpty());
    }

    // ── completion status persisted ───────────────────────────────────────────

    @Test
    @DisplayName("update() persists completion status change")
    void update_completionStatus_persisted() {
        AssignmentTask task = makeTask("T1");
        repo.save(task);
        task.setCompletionStatus(true);
        repo.update(task);
        assertTrue(repo.findById("T1").isCompletionStatus());
    }
}
