package com.studyplanner.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
 * StudyPlan - Represents a generated daily or weekly study schedule.
 * Stores a Map<LocalDate, List<Task>> (TreeMap for chronological order).
 * Provides methods to add tasks, query daily workload, detect overloaded days,
 * and attach AI recommendation notes.
 */
public class StudyPlan {

    private final Map<LocalDate, List<Task>> dailySchedule;
    private final String planTitle;
    private final LocalDate generatedOn;
    private final List<String> aiNotes;

    private static final int DAILY_LIMIT_MINUTES = 480; // 8 hours

    // Creates an empty study plan with the given title.
    public StudyPlan(String planTitle) {
        this.planTitle = planTitle;
        this.generatedOn = LocalDate.now();
        this.dailySchedule = new TreeMap<>();
        this.aiNotes = new ArrayList<>();
    }

    // Schedules a task on the given date.
    public void addTask(LocalDate date, Task task) {
        dailySchedule.computeIfAbsent(date, d -> new ArrayList<>()).add(task);
    }

    // Returns the tasks scheduled on the given date (empty list if none).
    public List<Task> getTasksForDate(LocalDate date) {
        return Collections.unmodifiableList(
                dailySchedule.getOrDefault(date, Collections.emptyList()));
    }

    // Returns the full schedule map (unmodifiable).
    public Map<LocalDate, List<Task>> getDailySchedule() {
        return Collections.unmodifiableMap(dailySchedule);
    }

    // Returns total estimated study minutes for the given date.
    public int getDailyMinutes(LocalDate date) {
        return dailySchedule.getOrDefault(date, Collections.emptyList())
                .stream().mapToInt(Task::getEstimatedMinutes).sum();
    }

    // Returns true if the given date exceeds the daily study limit.
    public boolean isDayOverloaded(LocalDate date) {
        return getDailyMinutes(date) > DAILY_LIMIT_MINUTES;
    }

    // Returns all dates where the daily limit is exceeded.
    public List<LocalDate> getOverloadedDays() {
        return dailySchedule.keySet().stream()
                .filter(this::isDayOverloaded)
                .sorted()
                .toList();
    }

    // Returns total number of tasks scheduled across all days.
    public int getTotalTaskCount() {
        return dailySchedule.values().stream().mapToInt(List::size).sum();
    }

    // Adds an AI recommendation note to the plan.
    public void addAiNote(String note) {
        if (note != null && !note.isBlank()) aiNotes.add(note.trim());
    }

    public List<String> getAiNotes()  { return Collections.unmodifiableList(aiNotes); }
    public String       getPlanTitle() { return planTitle; }
    public LocalDate    getGeneratedOn() { return generatedOn; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(planTitle).append(" | Generated: ").append(generatedOn).append(" ===\n");

        if (dailySchedule.isEmpty()) {
            sb.append("  (No tasks scheduled)\n");
            return sb.toString();
        }

        for (Map.Entry<LocalDate, List<Task>> entry : dailySchedule.entrySet()) {
            LocalDate date = entry.getKey();
            int totalMins = getDailyMinutes(date);
            sb.append("\n-- ").append(date.format(fmt))
              .append(" [").append(totalMins).append(" min]");
            if (isDayOverloaded(date)) sb.append(" *** OVERLOADED ***");
            sb.append("\n");
            for (int i = 0; i < entry.getValue().size(); i++) {
                Task t = entry.getValue().get(i);
                sb.append(String.format("  %d. [%s] %s (%d min, pri:%.1f)\n",
                        i + 1, t.getTaskType(), t.getTitle(),
                        t.getEstimatedMinutes(), t.calculatePriorityScore()));
            }
        }
        if (!aiNotes.isEmpty()) {
            sb.append("\n-- AI Notes --\n");
            for (String note : aiNotes) sb.append("  * ").append(note).append("\n");
        }
        sb.append("\nTotal tasks: ").append(getTotalTaskCount()).append("\n");
        if (!getOverloadedDays().isEmpty()) {
            sb.append("WARNING: ").append(getOverloadedDays().size())
              .append(" overloaded day(s) detected.\n");
        }
        return sb.toString();
    }
}
