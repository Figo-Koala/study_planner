package com.studyplanner.service;

import com.studyplanner.exception.InvalidTaskException;
import com.studyplanner.model.Task;
import com.studyplanner.repository.TaskRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/*
 * TaskService - Handles all core task lifecycle operations.
 * Validates task fields before delegating to TaskRepository for storage.
 * Operations: create, update, delete, mark complete/incomplete, retrieve.
 * Throws InvalidTaskException for validation failures.
 */
public class TaskService {

    private final TaskRepository taskRepository;

    // Takes a TaskRepository instance that must not be null.
    public TaskService(TaskRepository taskRepository) {
        if (taskRepository == null) throw new IllegalArgumentException("Repository must not be null.");
        this.taskRepository = taskRepository;
    }

    // Validates and saves a new task. Throws if any field is invalid or the ID already exists.
    public void createTask(Task task) throws InvalidTaskException {
        if (task == null) throw new InvalidTaskException("Task must not be null.", "task");
        validate(task);
        if (taskRepository.exists(task.getTaskId())) {
            throw new InvalidTaskException("Task ID '" + task.getTaskId() + "' already exists.", "taskId");
        }
        taskRepository.save(task);
    }

    // Returns the task with the given ID. Throws if not found or ID is blank.
    public Task getTask(String taskId) throws InvalidTaskException {
        if (taskId == null || taskId.isBlank())
            throw new InvalidTaskException("Task ID must not be blank.", "taskId");
        Task t = taskRepository.findById(taskId);
        if (t == null) throw new InvalidTaskException("No task found with ID: " + taskId, "taskId");
        return t;
    }

    // Returns all tasks currently stored.
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    // Validates and updates an existing task. Throws if invalid or task does not exist.
    public void updateTask(Task task) throws InvalidTaskException {
        if (task == null) throw new InvalidTaskException("Task must not be null.", "task");
        validate(task);
        if (!taskRepository.exists(task.getTaskId())) {
            throw new InvalidTaskException("No task found with ID: " + task.getTaskId(), "taskId");
        }
        taskRepository.update(task);
    }

    // Deletes the task with the given ID. Throws if not found.
    public void deleteTask(String taskId) throws InvalidTaskException {
        getTask(taskId); // throws if not found
        taskRepository.delete(taskId);
    }

    // Marks the task as completed.
    public void markComplete(String taskId) throws InvalidTaskException {
        Task t = getTask(taskId);
        t.setCompletionStatus(true);
        taskRepository.update(t);
    }

    // Marks the task as incomplete.
    public void markIncomplete(String taskId) throws InvalidTaskException {
        Task t = getTask(taskId);
        t.setCompletionStatus(false);
        taskRepository.update(t);
    }

    // Flips the completion status of the task.
    public void toggleCompletion(String taskId) throws InvalidTaskException {
        Task t = getTask(taskId);
        t.setCompletionStatus(!t.isCompletionStatus());
        taskRepository.update(t);
    }

    // Returns the total number of tasks stored.
    public int getTaskCount() { return taskRepository.count(); }

    // Returns true if a task with the given ID exists.
    public boolean taskExists(String taskId) {
        return taskId != null && taskRepository.exists(taskId);
    }

    /*
     * Validates all required fields of a task.
     * Throws InvalidTaskException naming the offending field if any check fails.
     */
    private void validate(Task task) throws InvalidTaskException {
        if (task.getTaskId() == null || task.getTaskId().isBlank())
            throw new InvalidTaskException("Task ID must not be blank.", "taskId");
        if (task.getTitle() == null || task.getTitle().isBlank())
            throw new InvalidTaskException("Title must not be empty.", "title");
        if (task.getCourseName() == null || task.getCourseName().isBlank())
            throw new InvalidTaskException("Course name must not be empty.", "courseName");
        if (task.getDeadline() == null)
            throw new InvalidTaskException("Deadline must not be null.", "deadline");
        if (task.getEstimatedMinutes() < 0)
            throw new InvalidTaskException("Estimated minutes must be >= 0.", "estimatedMinutes");
        if (task.getDifficulty() < 1 || task.getDifficulty() > 5)
            throw new InvalidTaskException("Difficulty must be 1-5.", "difficulty");
        if (task.getImportance() < 1 || task.getImportance() > 5)
            throw new InvalidTaskException("Importance must be 1-5.", "importance");
    }

    // Parses a date string in yyyy-MM-dd format. Throws InvalidTaskException on invalid format.
    public static LocalDate parseDate(String dateStr) throws InvalidTaskException {
        if (dateStr == null || dateStr.isBlank())
            throw new InvalidTaskException("Date must not be blank.", "deadline");
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidTaskException("Invalid date: '" + dateStr + "'. Use yyyy-MM-dd.", "deadline");
        }
    }
}
