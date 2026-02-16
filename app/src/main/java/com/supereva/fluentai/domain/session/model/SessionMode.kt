package com.supereva.fluentai.domain.session.model

/**
 * The type of speaking session partner.
 *
 * - [AI]    — conversation with the AI language coach.
 * - [HUMAN] — future real-time conversation with a human partner (RTC).
 */
enum class SessionMode {
    AI,
    HUMAN
}
