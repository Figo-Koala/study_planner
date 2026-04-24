package com.studyplanner.service;

import com.studyplanner.model.Task;
import com.studyplanner.repository.TaskRepository;

import java.util.*;
import java.util.stream.Collectors;

/*
 * TaskQueryService - Handles searching, sorting, grouping, and displaying tasks.
 * Uses Java stream operations to filter and transform collections.
 * All methods are read-only — they do not modify stored tasks.
 */
public class TaskQueryService {

    private final TaskRepository taskRepository;

    // Takes the TaskRepository to query. Must not be null.
    public TaskQueryService(TaskRepository taskRepository) {
        if (taskRepository == null) throw new IllegalArgumentException("Repository must not be null.");
        this.taskRepository = taskRepository;
    }

    // Returns all tasks in insertion order.
    public List<Task> getAllTasks() { return taskRepository.findAll(); }

    // Returns all tasks sorted by deadline (earliest first), ties broken by title.
    public List<Task> getAllTasksSortedByDeadline() {
        return taskRepository.findAll().stream()
                .sorted(Comparator.comparing(Task::getDeadline).thenComparing(Task::getTitle))
                .collect(Collectors.toList());
    }

    // Returns all tasks sorted by priority score (highest first), ties by deadline.
    public List<Task> getAllTasksSortedByPriority() {
        return taskRepository.findAll().stream()
                .sorted(Comparator.comparingDouble(Task::calculatePriorityScore)
                        .reversed().thenComparing(Task::getDeadline))
                .collect(Collectors.toList());
    }

    // Returns tasks for the given course (case-insensitive), sorted by deadline.
    public List<Task> filterByCourseName(String courseName) {
        if (courseName == null || courseName.isBlank()) return Collections.emptyList();
        return taskRepository.findAll().stream()
                .filter(t -> t.getCourseName().equalsIgnoreCase(courseName.trim()))
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.toList());
    }

    // Returns tasks filtered by completion status, sorted by deadline.
    public List<Task> filterByCompletionStatus(boolean completed) {
        return taskRepository.findAll().stream()
                .filter(t -> t.isCompletionStatus() == completed)
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.toList());
    }

    // Returns tasks filtered by type string (e.g. "EXAM"), case-insensitive.
    public List<Task> filterByTaskType(String taskType) {
        if (taskType == null || taskType.isBlank()) return Collections.emptyList();
        return taskRepository.findAll().stream()
                .filter(t -> t.getTaskType().equalsIgnoreCase(taskType.trim()))
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.toList());
    }

    // Groups all tasks by course name. Each group is sorted by deadline.
    public Map<String, List<Task>> groupByCourse() {
        return taskRepository.findAll().stream()
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.groupingBy(Task::getCourseName, TreeMap::new, Collectors.toList()));
    }

    // Groups all tasks by task type. Each group is sorted by deadline.
    public Map<String, List<Task>> groupByTaskType() {
        return taskRepository.findAll().stream()
                .sorted(Comparator.comparing(Task::getDeadline))
                .collect(Collectors.groupingBy(Task::getTaskType, TreeMap::new, Collectors.toList()));
    }

    // Returns the top N incomplete tasks by priority score.
    public List<Task> getTopPriorityTasks(int n) {
        return taskRepository.findAll().stream()
                .filter(t -> !t.isCompletionStatus())
                .sorted(Comparator.comparingDouble(Task::calculatePriorityScore).reversed())
                .limit(Math.max(0, n))
                .collect(Collectors.toList());
    }

    // Returns the count of pending (incomplete) tasks.
    public long countPendingTasks() {
        return taskRepository.findAll().stream().filter(t -> !t.isCompletionStatus()).count();
    }

    // Returns the count of completed tasks.
    public long countCompletedTasks() {
        return taskRepository.findAll().stream().filter(Task::isCompletionStatus).count();
    }

    // Returns sorted list of unique course names.
    public List<String> getDistinctCourseNames() {
        return taskRepository.findAll().stream()
                .map(Task::getCourseName).distinct().sorted().collect(Collectors.toList());
    }

    // Prints a numbered task list to standard output with the given header.
    public void printTaskList(List<Task> tasks, String header) {
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  " + header);
        System.out.println("=".repeat(65));
        if (tasks.isEmpty()) {
            System.out.println("  (No tasks)");
        } else {
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                String status = t.isCompletionStatus() ? "v" : "o";
                System.out.printf("  %2d. %s [%-10s] %-30s Due:%-12s Pri:%5.1f%n",
                        i + 1, status, t.getTaskType(),
                        truncate(t.getTitle(), 30), t.getDeadline(),
                        t.calculatePriorityScore());
            }
        }
        System.out.println("=".repeat(65));
        System.out.println("  Total: " + tasks.size());
    }

    // Prints tasks grouped by a label to standard output with the given header.
    public void printGroupedTasks(Map<String, List<Task>> grouped, String header) {
        System.out.println("\n" + "=".repeat(65));
        System.out.println("  " + header);
        System.out.println("=".repeat(65));
        if (grouped.isEmpty()) {
            System.out.println("  (No tasks)");
        } else {
            for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
                System.out.println("\n  -- " + entry.getKey() + " (" + entry.getValue().size() + ") --");
                for (Task t : entry.getValue()) {
                    String status = t.isCompletionStatus() ? "v" : "o";
                    System.out.printf("     %s %-30s Due:%-12s Pri:%5.1f%n",
                            status, truncate(t.getTitle(), 30), t.getDeadline(),
                            t.calculatePriorityScore());
                }
            }
        }
        System.out.println("=".repeat(65));
    }

    // Truncates string to max characters, appending "..." if cut.
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }
}
