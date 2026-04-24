package com.studyplanner.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * ExamTask - Represents an exam preparation task (quiz, midterm, final).
 * Extends Task with: topicList (List<String>) and isCumulative flag.
 * Overrides calculatePriorityScore() to add +10 for cumulative exams,
 * plus +2 per topic beyond the first three, capped at +6.
 */
public class ExamTask extends Task {

    private List<String> topicList;
    private boolean isCumulative;

    public ExamTask(String taskId, String title, String courseName,
                    LocalDate deadline, int estimatedMinutes,
                    int difficulty, int importance,
                    List<String> topicList, boolean isCumulative) {
        super(taskId, title, courseName, deadline, estimatedMinutes, difficulty, importance);
        this.topicList = topicList != null ? new ArrayList<>(topicList) : new ArrayList<>();
        this.isCumulative = isCumulative;
    }

    // Convenience constructor with empty topic list and non-cumulative.
    public ExamTask(String taskId, String title, String courseName,
                    LocalDate deadline, int estimatedMinutes,
                    int difficulty, int importance) {
        this(taskId, title, courseName, deadline, estimatedMinutes,
             difficulty, importance, new ArrayList<>(), false);
    }

    @Override
    public String getTaskType() { return "EXAM"; }

    @Override
    public String getTypeSpecificDetails() {
        String topics = topicList.isEmpty() ? "(none)" : String.join(", ", topicList);
        return "Topics: [" + topics + "] | Cumulative: " + (isCumulative ? "Yes" : "No");
    }

    // Adds +10 for cumulative exams, +2 per extra topic beyond the first three.
    @Override
    public double calculatePriorityScore() {
        double base = super.calculatePriorityScore();
        if (isCumulative) base = Math.min(100.0, base + 10.0);
        int extraTopics = Math.max(0, topicList.size() - 3);
        return Math.min(100.0, base + extraTopics * 2.0);
    }

    // Adds a topic; ignores blank or null entries.
    public void addTopic(String topic) {
        if (topic != null && !topic.isBlank()) topicList.add(topic.trim());
    }

    // Removes a topic by exact name.
    public void removeTopic(String topic) { topicList.remove(topic); }

    // Returns an unmodifiable view of the topic list.
    public List<String> getTopicList() { return Collections.unmodifiableList(topicList); }

    public void setTopicList(List<String> topicList) {
        this.topicList = topicList != null ? new ArrayList<>(topicList) : new ArrayList<>();
    }

    public boolean isCumulative()                    { return isCumulative; }
    public void    setCumulative(boolean cumulative) { this.isCumulative = cumulative; }

    @Override
    public String toString() {
        return super.toString() + " | " + getTypeSpecificDetails();
    }
}
