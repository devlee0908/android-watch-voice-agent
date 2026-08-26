package com.haesung.watchvoice.phone.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.haesung.watchvoice.protocol.CommandEnvelope
import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.FailureReason
import com.haesung.watchvoice.protocol.MessagePaths
import com.haesung.watchvoice.protocol.WatchCommand
import com.haesung.watchvoice.protocol.decodeEnvelope
import com.haesung.watchvoice.protocol.encode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class CommandListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appLauncher by lazy { AndroidAppLauncher(this) }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != MessagePaths.COMMAND) return

        val envelope = runCatching { decodeEnvelope(event.data) }.getOrElse { e ->
            Timber.e(e, "Undecodable command from %s", event.sourceNodeId)
            return
        }

        scope.launch { reply(event.sourceNodeId, handle(envelope)) }
    }

    private suspend fun handle(envelope: CommandEnvelope): CommandResult {
        val id = envelope.commandId
        if (envelope.protocolVersion != CommandEnvelope.PROTOCOL_VERSION) {
            return CommandResult.Failure(id, FailureReason.UNSUPPORTED_PROTOCOL_VERSION)
        }
        val age = System.currentTimeMillis() - envelope.sentAtEpochMs
        if (age > CommandEnvelope.MAX_AGE_MS) {
            return CommandResult.Failure(id, FailureReason.STALE_COMMAND, detail = "${age}ms old")
        }

        return when (val command = envelope.command) {
            is WatchCommand.Ping -> CommandResult.Success(id, message = "pong")
            is WatchCommand.LaunchApp -> when (val outcome = appLauncher.launch(command.appKey)) {
                is com.haesung.watchvoice.phone.domain.LaunchOutcome.Launched ->
                    CommandResult.Success(id, message = outcome.label)
                com.haesung.watchvoice.phone.domain.LaunchOutcome.NotInstalled ->
                    CommandResult.Failure(id, FailureReason.APP_NOT_INSTALLED)
                com.haesung.watchvoice.phone.domain.LaunchOutcome.NotLaunchable ->
                    CommandResult.Failure(id, FailureReason.APP_NOT_LAUNCHABLE)
                is com.haesung.watchvoice.phone.domain.LaunchOutcome.Ambiguous ->
                    CommandResult.Failure(
                        id,
                        FailureReason.APP_AMBIGUOUS,
                        detail = outcome.candidateLabels.joinToString(),
                    )
                is com.haesung.watchvoice.phone.domain.LaunchOutcome.BlockedNeedsUserTap ->
                    CommandResult.Failure(
                        id,
                        FailureReason.LAUNCH_BLOCKED_NEEDS_USER_TAP,
                        detail = outcome.label,
                    )
            }
            is WatchCommand.CreateCalendarEvent,
            is WatchCommand.UpdateCalendarEvent,
            -> CommandResult.Failure(id, FailureReason.UNSUPPORTED_COMMAND)
        }
    }

    private suspend fun reply(nodeId: String, result: CommandResult) {
        runCatching {
            Wearable.getMessageClient(this)
                .sendMessage(nodeId, MessagePaths.RESULT, result.encode())
                .await()
        }.onFailure { Timber.e(it, "Failed to reply to %s", nodeId) }
    }
}
