package com.supereva.fluentai.domain.model


data class PracticeResult(
        val transcript: String,
        val correctedText: String,
        val feedback: String,
        val score: Int
)


