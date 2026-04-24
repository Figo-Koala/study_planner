package com.studyplanner.ai;

import com.studyplanner.model.ExamTask;
import com.studyplanner.model.ReadingTask;
import com.studyplanner.model.Task;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/*
 * MockAIRecommendationService - Offline implementation of AIRecommendationService.
 * Produces deterministic, rule-based responses that mimic real AI output.
 * Branches on task type, deadline urgency, difficulty, and importance.
 * No network calls or API key required. Default mode for the application.
 */
public class MockAIRecommendationService implements AIRecommendationService {

    @Override
    public String getModeName()   { return "Offline Mock Mode"; }

    @Override
    public boolean isOnlineMode() { return false; }

    // Returns a priority explanation based on deadline urgency and difficulty.
    @Override
    public String generatePriorityExplanation(Task task) {
        if (task == null) return "No task provided.";
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadline());
        double score   = task.calculatePriorityScore();

        String urgency = daysUntil < 0  ? "OVERDUE" :
                         daysUntil == 0 ? "due TODAY" :
                         daysUntil <= 3 ? "due in " + daysUntil + " day(s) - urgent" :
                                          "due in " + daysUntil + " day(s)";

        String level = score >= 80 ? "CRITICAL" : score >= 60 ? "HIGH" :
                       score >= 40 ? "MEDIUM"   : "LOW";

        String advice = daysUntil <= 0 ? "Address this immediately." :
                        daysUntil <= 3 && task.getDifficulty() >= 4
                            ? "Start now — high difficulty with little time remaining." :
                        task.getImportance() >= 4
                            ? "High importance — prioritise over lower-stakes work." :
                        task.getEstimatedMinutes() > 180
                            ? "Large task — break into shorter daily sessions." :
                            "Work steadily to avoid last-minute pressure.";

        return trim("Priority: " + level + " (score: " + String.format("%.1f", score) +
                "). Deadline: " + task.getDeadline() + " (" + urgency + "). " + advice);
    }

    // Returns a bullet-point breakdown tailored to the task type.
    @Override
    public String generateBreakdown(Task task) {
        if (task == null) return "No task provided.";
        return switch (task.getTaskType()) {
            case "ASSIGNMENT" -> trim(
                "Steps for \"" + task.getTitle() + "\":\n" +
                "  * Read the requirements and rubric carefully\n" +
                "  * Outline your approach before writing\n" +
                "  * Complete a rough draft first\n" +
                "  * Review and revise before submission\n" +
                "  * Submit before the deadline (" + task.getDeadline() + ")");
            case "EXAM" -> {
                String topicsHint = "";
                if (task instanceof ExamTask et && !et.getTopicList().isEmpty()) {
                    topicsHint = " Focus on: " + String.join(", ", et.getTopicList()) + ".";
                }
                yield trim(
                "Steps for \"" + task.getTitle() + "\":\n" +
                "  * Gather notes, slides, and past quizzes\n" +
                "  * Identify the most tested concepts\n" +
                "  * Review each topic section by section\n" +
                "  * Practice with example problems or flashcards\n" +
                "  * Do a final review the day before (" + task.getDeadline() + ")" + topicsHint);
            }
            case "READING" -> {
                String pageHint = "";
                if (task instanceof ReadingTask rt && rt.getPageCount() > 0) {
                    pageHint = " Aim for ~" + Math.max(10, rt.getPageCount() / 4) + " pages per session.";
                }
                yield trim(
                "Steps for \"" + task.getTitle() + "\":\n" +
                "  * Preview headings and summaries first\n" +
                "  * Read actively and take notes\n" +
                "  * Break into multiple sessions\n" +
                "  * Summarise each section in your own words\n" +
                "  * Review notes after finishing (" + task.getDeadline() + ")" + pageHint);
            }
            default -> trim(
                "Steps for \"" + task.getTitle() + "\":\n" +
                "  * Define what 'done' looks like\n" +
                "  * Break into 3-5 manageable sub-tasks\n" +
                "  * Tackle the hardest part first\n" +
                "  * Reserve time for review before " + task.getDeadline());
        };
    }

    // Trims output to 500 characters max.
    private String trim(String s) {
        if (s == null || s.isBlank()) return "No recommendation available.";
        s = s.trim();
        return s.length() > 500 ? s.substring(0, 497) + "..." : s;
    }
}
