package com.haesung.watchvoice.watch.domain

import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.WatchCommand

/**
 * Sends a command to the phone companion and waits for its result.
 *
 * The domain never sees the Wearable Data Layer; [com.haesung.watchvoice.watch.data] provides the
 * only implementation that does.
 */
interface CommandTransport {
    suspend fun send(command: WatchCommand): TransportOutcome
}

sealed interface TransportOutcome {
    data class Completed(val result: CommandResult) : TransportOutcome

    /** No paired phone reachable, or the companion app is not installed on it. */
    data object CompanionUnreachable : TransportOutcome

    data class DeliveryFailed(val detail: String?) : TransportOutcome

    /** Delivered, but the phone never answered within the timeout. */
    data object TimedOut : TransportOutcome
}
