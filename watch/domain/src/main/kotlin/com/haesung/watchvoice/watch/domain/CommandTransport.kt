package com.haesung.watchvoice.watch.domain

import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.WatchCommand

/**
 * Sends a command to the phone agent and waits for its result.
 *
 * The domain never sees the Wearable Data Layer; [com.haesung.watchvoice.watch.data] provides the
 * only implementation that does.
 */
interface CommandTransport {
    suspend fun send(command: WatchCommand): TransportOutcome
}

sealed interface TransportOutcome {
    data class Completed(val result: CommandResult) : TransportOutcome

    /** No paired phone reachable, or the agent app is not installed on it. */
    data object AgentUnreachable : TransportOutcome

    data class DeliveryFailed(val detail: String?) : TransportOutcome

    /** Delivered, but the phone never answered within the timeout. */
    data object TimedOut : TransportOutcome
}
