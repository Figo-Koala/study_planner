package com.studyplanner.ai;

import com.studyplanner.model.Task;

/*
 * OnlineAIRecommendationService - Stub implementation of AIRecommendationService using OpenAI.
 * Stores an API key and constructs prompts for priority explanation and task breakdown.
 * The actual HTTP call in callOpenAI() is left as a TODO — the rest of the class is wired up.
 * Falls back to informative stub messages when no API key is provided.
 * To activate: add openai-java dependency to pom.xml, put key in config.properties, implement callOpenAI().
 */
public class OnlineAIRecommendationService implements AIRecommendationService {

    private static final int MAX_LENGTH = 300;

    private final String apiKey;

    // Creates the service with the given API key. Null or blank key puts it in stub mode.
    public OnlineAIRecommendationService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getModeName() {
        return isConfigured() ? "Online OpenAI Mode" : "Online Mode (no API key)";
    }

    @Override
    public boolean isOnlineMode() { return true; }

    // Returns a priority explanation from OpenAI, or a stub message if not configured.
    @Override
    public String generatePriorityExplanation(Task task) {
        if (task == null) return "No task provided.";
        if (!isConfigured()) return stubMessage("priority explanation", task);
        try {
            return validate(callOpenAI(buildPriorityPrompt(task)));
        } catch (Exception e) {
            return "AI error — switch to offline mode or check your API key.";
        }
    }

    // Returns a step-by-step breakdown from OpenAI, or a stub message if not configured.
    @Override
    public String generateBreakdown(Task task) {
        if (task == null) return "No task provided.";
        if (!isConfigured()) return stubMessage("task breakdown", task);
        try {
            return validate(callOpenAI(buildBreakdownPrompt(task)));
        } catch (Exception e) {
            return "AI error — switch to offline mode or check your API key.";
        }
    }

    // Builds a prompt asking for a 2-3 sentence priority explanation.
    private String buildPriorityPrompt(Task task) {
        return String.format(
            "You are a study advisor. In 2-3 sentences explain the priority of this task and what to do next.%n" +
            "Task: \"%s\" | Type: %s | Course: %s | Deadline: %s | Difficulty: %d/5 | Importance: %d/5 | Score: %.1f",
            task.getTitle(), task.getTaskType(), task.getCourseName(),
            task.getDeadline(), task.getDifficulty(), task.getImportance(),
            task.calculatePriorityScore());
    }

    // Builds a prompt asking for 4-6 concise bullet points to approach the task.
    private String buildBreakdownPrompt(Task task) {
        return String.format(
            "You are a study advisor. Give exactly 4-6 bullet points on how to approach this task. " +
            "Each bullet is one short actionable sentence.%n" +
            "Task: \"%s\" | Type: %s | Course: %s | Deadline: %s | Difficulty: %d/5 | Est. time: %d min",
            task.getTitle(), task.getTaskType(), task.getCourseName(),
            task.getDeadline(), task.getDifficulty(), task.getEstimatedMinutes());
    }

    /*
     * Sends the prompt to the OpenAI API and returns the response text.
     * TODO: Replace body with actual OpenAI Java SDK call:
     *   OpenAIClient client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
     *   Response r = client.responses().create(ResponseCreateParams.builder()
     *       .input(prompt).model(ChatModel.GPT_4O_MINI).build());
     *   return r.output()... (collect text);
     */
    private String callOpenAI(String prompt) {
        throw new UnsupportedOperationException(
            "Online AI not connected. Add API key to config.properties and implement callOpenAI().");
    }

    // Returns true if a usable API key is present.
    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // Trims the response to MAX_LENGTH characters; returns fallback for null/blank.
    private String validate(String response) {
        if (response == null || response.isBlank()) return "AI returned an empty response.";
        String s = response.trim();
        return s.length() > MAX_LENGTH ? s.substring(0, MAX_LENGTH - 3) + "..." : s;
    }

    // Returns a user-friendly message explaining that online AI is not configured.
    private String stubMessage(String feature, Task task) {
        return "[Online AI not configured] To enable " + feature + " for \"" + task.getTitle() +
               "\", add your OpenAI API key to config.properties.";
    }
}
