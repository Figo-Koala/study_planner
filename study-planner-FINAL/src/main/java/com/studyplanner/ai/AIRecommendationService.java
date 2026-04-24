package com.studyplanner.ai;

import com.studyplanner.model.Task;

/*
 * AIRecommendationService - Interface for AI-powered task recommendations.
 * Defines two methods: generatePriorityExplanation and generateBreakdown.
 * Implementations: MockAIRecommendationService (offline), OnlineAIRecommendationService (OpenAI).
 * The application works fully without online AI — mock mode is always available.
 */
public interface AIRecommendationService {

    // Returns a short explanation of why the task has its priority score.
    String generatePriorityExplanation(Task task);

    // Returns a bullet-point list of steps to approach the task.
    String generateBreakdown(Task task);

    // Returns the display name of this AI mode.
    String getModeName();

    // Returns true if this implementation requires internet and an API key.
    boolean isOnlineMode();
}
