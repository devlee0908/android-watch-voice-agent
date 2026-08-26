package com.haesung.watchvoice.watch.data

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.haesung.watchvoice.protocol.CommandEnvelope
import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.MessagePaths
import com.haesung.watchvoice.protocol.WatchCommand
import com.haesung.watchvoice.protocol.decodeResult
import com.haesung.watchvoice.protocol.encode
import com.haesung.watchvoice.watch.domain.CommandTransport
import com.haesung.watchvoice.watch.domain.TransportOutcome
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber

/**
 * Sends commands over [MessageClient] and awaits the phone's reply on [MessagePaths.RESULT].
 *
 * `MessageClient` — not `DataClient` — carries commands: data items are de-duplicated and replayed
 * on reconnect, which for "launch Spotify" would mean an app opening minutes later out of nowhere.
 */
class WearableCommandTransport(
    context: Context,
    private val messageClient: MessageClient = Wearable.getMessageClient(context),
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context),
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) : CommandTransport {

    override suspend fun send(command: WatchCommand): TransportOutcome {
        val nodeId = findAgentNode() ?: return TransportOutcome.AgentUnreachable
        val envelope = CommandEnvelope(
            commandId = UUID.randomUUID().toString(),
            sentAtEpochMs = System.currentTimeMillis(),
            command = command,
        )

        val reply = CompletableDeferred<CommandResult>()
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            if (event.path != MessagePaths.RESULT) return@OnMessageReceivedListener
            val result = runCatching { decodeResult(event.data) }.getOrNull()
            if (result != null && result.commandId == envelope.commandId) {
                reply.complete(result)
            }
        }
        // Subscribe before sending so a fast reply cannot arrive before we are listening.
        messageClient.addListener(listener).await()

        return try {
            withTimeout(timeoutMs) {
                messageClient.sendMessage(nodeId, MessagePaths.COMMAND, envelope.encode()).await()
                TransportOutcome.Completed(reply.await())
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w(e, "No reply for command %s", envelope.commandId)
            TransportOutcome.TimedOut
        } catch (e: Exception) {
            Timber.e(e, "Failed to deliver command %s", envelope.commandId)
            TransportOutcome.DeliveryFailed(e.message)
        } finally {
            messageClient.removeListener(listener)
        }
    }

    private suspend fun findAgentNode(): String? {
        val capability = capabilityClient
            .getCapability(MessagePaths.AGENT_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
        return capability.nodes.firstOrNull { it.isNearby }?.id ?: capability.nodes.firstOrNull()?.id
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS: Long = 10_000
    }
}
