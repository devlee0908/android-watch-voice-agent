package com.haesung.watchvoice.protocol

import kotlinx.serialization.Serializable

@Serializable
data class CommandEnvelope(
    val commandId: String,
    val protocolVersion: Int = PROTOCOL_VERSION,
    val sentAtEpochMs: Long,
    val command: WatchCommand,
) {
    companion object {
        const val PROTOCOL_VERSION: Int = 1

        /** Envelopes older than this are rejected, so a queued command cannot fire on reconnect. */
        const val MAX_AGE_MS: Long = 60_000
    }
}
