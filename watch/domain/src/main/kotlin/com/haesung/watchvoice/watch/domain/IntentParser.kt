package com.haesung.watchvoice.watch.domain

import com.haesung.watchvoice.protocol.WatchCommand

/**
 * Turns a speech transcript into a [WatchCommand]. Implemented by a rule-based parser on the watch
 * and, later, by an LLM-backed parser that runs on the phone; callers depend only on this port.
 */
interface IntentParser {
    suspend fun parse(transcript: String, context: ParseContext): ParseOutcome
}

/** Everything time- and locale-dependent a parser needs, passed in so parsers stay pure. */
data class ParseContext(
    val nowEpochMs: Long,
    val timeZoneId: String,
    val languageTag: String,
)

sealed interface ParseOutcome {
    data class Parsed(val command: WatchCommand, val confidence: Float) : ParseOutcome

    /** The utterance was understood but is missing something the user must supply. */
    data class NeedsClarification(val question: String) : ParseOutcome

    data object NotUnderstood : ParseOutcome
}
