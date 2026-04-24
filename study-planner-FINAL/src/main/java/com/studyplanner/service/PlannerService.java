package com.studyplanner.service;

import com.studyplanner.ai.AIRecommendationService;
import com.studyplanner.exception.SchedulingConflictException;
import com.studyplanner.model.StudyPlan;
import com.studyplanner.model.Task;
import com.studyplanner.repository.TaskRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/*
 * PlannerService - Calculates task priorities and builds StudyPlan objects.
 * Uses a greedy scheduling algorithm: sorts incomplete tasks by deadline and
 * priority, then assigns each task to the earliest available day within the
 * study window that has remaining capacity.
 * Optionally attaches AI recommendation notes if an AIRecommendationService
 * is set. The plan is fully functional without AI.
 * Throws SchedulingConflictException when overloaded days are detected.
 */
public class PlannerService {

    public static final int DEFAULT_DAILY_LIMIT = 480; // 8 hours
    public static final int DEFAULT_PLAN_DAYS   = 7;

    private final TaskRepository taskRepository;
    private AIRecommendationService aiService;
    private int dailyLimitMinutes;

    // Creates a PlannerService with no AI service and the default daily limit.
    public PlannerService(TaskRepository taskRepository) {
        this(taskRepository, null, DEFAULT_DAILY_LIMIT);
    }

    // Creates a PlannerService with an optional AI service and custom daily limit.
    public PlannerService(TaskRepository taskRepository,
                          AIRecommendationService aiService, int dailyLimit) {
        if (taskRepository == null) throw new IllegalArgumentException("Repository must not be null.");
        this.taskRepository = taskRepository;
        this.aiService = aiService;
        this.dailyLimitMinutes = dailyLimit > 0 ? dailyLimit : DEFAULT_DAILY_LIMIT;
    }

    // Generates a 7-day study plan without AI notes.
    public StudyPlan generateStudyPlan() throws SchedulingConflictException {
        return generateStudyPlan(DEFAULT_PLAN_DAYS, false);
    }

    /*
     * Generates a study plan for the given number of days.
     * If includeAiNotes is true and an AI service is set, attaches notes for top tasks.
     * Throws SchedulingConflictException if any day exceeds the daily limit.
     */
    public StudyPlan generateStudyPlan(int days, boolean includeAiNotes)
            throws SchedulingConflictException {
        int planDays = Math.max(1, days);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(planDays - 1);
        StudyPlan plan = new StudyPlan(planDays + "-Day Study Plan");

        // Collect incomplete tasks sorted by deadline then priority
        List<Task> tasks = taskRepository.findAll().stream()
                .filter(t -> !t.isCompletionStatus())
                .sorted(Comparator.comparing(Task::getDeadline)
                        .thenComparingDouble(t -> -t.calculatePriorityScore()))
                .collect(Collectors.toList());

        if (tasks.isEmpty()) return plan;

        // Track remaining capacity per day
        Map<LocalDate, Integer> capacity = new LinkedHashMap<>();
        for (long i = 0; i < planDays; i++) capacity.put(today.plusDays(i), dailyLimitMinutes);

        // Greedy: assign each task to the earliest day it fits
        for (Task task : tasks) {
            LocalDate effectiveEnd = task.getDeadline().isBefore(endDate) ? task.getDeadline() : endDate;
            boolean scheduled = false;
            for (LocalDate date : capacity.keySet()) {
                if (date.isAfter(effectiveEnd)) continue;
                int rem = capacity.getOrDefault(date, 0);
                if (rem >= task.getEstimatedMinutes()) {
                    plan.addTask(date, task);
                    capacity.put(date, rem - task.getEstimatedMinutes());
                    scheduled = true;
                    break;
                }
            }
            // If no day fits, place on the deadline day anyway (will flag as overloaded)
            if (!scheduled) {
                LocalDate fallback = effectiveEnd.isBefore(today) ? today : effectiveEnd;
                if (capacity.containsKey(fallback)) {
                    plan.addTask(fallback, task);
                    capacity.put(fallback, capacity.get(fallback) - task.getEstimatedMinutes());
                }
            }
        }

        if (includeAiNotes) attachAiNotes(plan, tasks);

        // Check for overloaded days
        List<LocalDate> overloaded = plan.getOverloadedDays();
        if (!overloaded.isEmpty()) {
            throw new SchedulingConflictException(
                    overloaded.size() + " day(s) exceed the " + dailyLimitMinutes + "-min daily limit.",
                    overloaded, dailyLimitMinutes);
        }
        return plan;
    }

    // Convenience: generates a plan with AI notes attached.
    public StudyPlan generateStudyPlanWithAI(int days) throws SchedulingConflictException {
        return generateStudyPlan(days, true);
    }

    // Returns the priority score for a given task.
    public double calculatePriority(Task task) {
        return task == null ? 0.0 : task.calculatePriorityScore();
    }

    // Returns a text summary of daily workload totals for the given plan.
    public String generateWorkloadSummary(StudyPlan plan) {
        if (plan.getDailySchedule().isEmpty()) return "No tasks scheduled.";
        StringBuilder sb = new StringBuilder("Daily Workload:\n");
        plan.getDailySchedule().forEach((date, tasks) -> {
            int mins = plan.getDailyMinutes(date);
            sb.append(String.format("  %s: %d min %s%n",
                    date, mins, plan.isDayOverloaded(date) ? "(OVER LIMIT)" : ""));
        });
        return sb.toString();
    }

    // Attaches AI notes for the top tasks (up to 3). Silently skips on error.
    private void attachAiNotes(StudyPlan plan, List<Task> tasks) {
        if (aiService == null) {
            plan.addAiNote("No AI service active. Switch to mock or online mode.");
            return;
        }
        int max = Math.min(3, tasks.size());
        for (int i = 0; i < max; i++) {
            Task t = tasks.get(i);
            try {
                String note = aiService.generatePriorityExplanation(t);
                if (note != null && !note.isBlank()) {
                    plan.addAiNote("[" + t.getTitle() + "] " + note.trim());
                }
            } catch (Exception e) {
                plan.addAiNote("AI note unavailable for: " + t.getTitle());
            }
        }
    }

    public AIRecommendationService getAiService()               { return aiService; }
    public void setAiService(AIRecommendationService aiService) { this.aiService = aiService; }
    public int  getDailyLimitMinutes()                          { return dailyLimitMinutes; }
    public void setDailyLimitMinutes(int limit) {
        this.dailyLimitMinutes = limit > 0 ? limit : DEFAULT_DAILY_LIMIT;
    }
}
