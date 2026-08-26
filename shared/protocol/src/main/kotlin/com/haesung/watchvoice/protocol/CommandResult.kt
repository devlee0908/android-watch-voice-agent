package com.haesung.watchvoice.protocol

import kotlinx.serialization.Serializable

@Serializable
sealed interface CommandResult {
    val commandId: String

    @Serializable
    data class Success(
        override val commandId: String,
        val message: String? = null,
    ) : CommandResult

    @Serializable
    data class Failure(
        override val commandId: String,
        val reason: FailureReason,
        val detail: String? = null,
    ) : CommandResult
}

@Serializable
enum class FailureReason {
    APP_NOT_INSTALLED,
    APP_NOT_LAUNCHABLE,
    LAUNCH_BLOCKED_NEEDS_USER_TAP,
    CALENDAR_PERMISSION_DENIED,
    NO_WRITABLE_CALENDAR,
    EVENT_NOT_FOUND,
    EVENT_AMBIGUOUS,
    INVALID_EVENT_TIME,
    UNSUPPORTED_COMMAND,
    UNSUPPORTED_PROTOCOL_VERSION,
    STALE_COMMAND,
    INTERNAL_ERROR,
}
