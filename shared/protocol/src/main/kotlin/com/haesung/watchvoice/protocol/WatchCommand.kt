package com.haesung.watchvoice.protocol

import kotlinx.serialization.Serializable

/**
 * A single imperative request travelling from the watch to the phone companion.
 *
 * The set of commands is intentionally closed and carries no Android types: a forged or replayed
 * message cannot express an arbitrary component, action, or extra.
 */
@Serializable
sealed interface WatchCommand {

    /** Liveness probe used to verify the transport without side effects. */
    @Serializable
    data object Ping : WatchCommand

    /**
     * Launch a phone application. [appKey] is a logical identifier such as `"spotify"`; the phone
     * is the only side that can resolve it to an installed package.
     */
    @Serializable
    data class LaunchApp(val appKey: String) : WatchCommand

    @Serializable
    data class CreateCalendarEvent(val event: CalendarEventDraft) : WatchCommand

    @Serializable
    data class UpdateCalendarEvent(val eventId: Long, val patch: CalendarEventPatch) : WatchCommand
}

@Serializable
data class CalendarEventDraft(
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    /** Resolved on the watch and sent explicitly so the phone never has to infer it. */
    val timeZoneId: String,
    val location: String? = null,
    val description: String? = null,
)

@Serializable
data class CalendarEventPatch(
    val title: String? = null,
    val startEpochMs: Long? = null,
    val endEpochMs: Long? = null,
    val timeZoneId: String? = null,
    val location: String? = null,
    val description: String? = null,
)
