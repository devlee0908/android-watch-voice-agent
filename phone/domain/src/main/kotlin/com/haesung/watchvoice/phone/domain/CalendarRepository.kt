package com.haesung.watchvoice.phone.domain

import com.haesung.watchvoice.protocol.CalendarEventDraft
import com.haesung.watchvoice.protocol.CalendarEventPatch

/**
 * The only door to the system calendar. `CalendarContract` lives behind this port, in
 * [com.haesung.watchvoice.phone.data], so nothing else needs the calendar permissions.
 */
interface CalendarRepository {
    suspend fun create(draft: CalendarEventDraft): CalendarWriteOutcome

    suspend fun update(eventId: Long, patch: CalendarEventPatch): CalendarWriteOutcome
}

sealed interface CalendarWriteOutcome {
    data class Written(val eventId: Long) : CalendarWriteOutcome

    data object PermissionDenied : CalendarWriteOutcome

    data object NoWritableCalendar : CalendarWriteOutcome

    data object EventNotFound : CalendarWriteOutcome

    data class InvalidTime(val detail: String) : CalendarWriteOutcome
}
