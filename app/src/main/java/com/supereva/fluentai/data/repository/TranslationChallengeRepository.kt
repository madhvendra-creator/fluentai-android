package com.supereva.fluentai.data.repository

/**
 * Provides static, offline translation challenges.
 * In a real app, this might come from a remote database or dynamic LLM generation.
 */
object TranslationChallengeRepository {
    
    private val challenges = listOf(
        "Hello, how are you today?",
        "Where is the nearest train station?",
        "I would like to order a cup of coffee.",
        "Could you please help me with these bags?",
        "What time does the meeting start tomorrow?",
        "I need a taxi to the airport immediately.",
        "How much does this item cost?",
        "Do you have any vegetarian options on the menu?",
        "It's very nice to meet you.",
        "I am learning how to speak a new language."
    )

    /**
     * Gets a random English challenge sentence for translation practice.
     */
    fun getRandomChallenge(): String {
        return challenges.random()
    }
}
