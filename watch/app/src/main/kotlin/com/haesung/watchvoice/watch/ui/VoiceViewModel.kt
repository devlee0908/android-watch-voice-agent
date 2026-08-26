package com.haesung.watchvoice.watch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.FailureReason
import com.haesung.watchvoice.watch.domain.CommandTransport
import com.haesung.watchvoice.watch.domain.IntentParser
import com.haesung.watchvoice.watch.domain.ParseContext
import com.haesung.watchvoice.watch.domain.ParseOutcome
import com.haesung.watchvoice.watch.domain.TransportOutcome
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data object Sending : VoiceState
    data class Launched(val label: String) : VoiceState
    data object NotUnderstood : VoiceState
    data object AppNotInstalled : VoiceState
    data object AppNotLaunchable : VoiceState
    data class Ambiguous(val candidates: String) : VoiceState
    data class NeedsUserTap(val detail: String?) : VoiceState
    data object AgentUnreachable : VoiceState
    data object TimedOut : VoiceState
    data class DeliveryFailed(val detail: String?) : VoiceState
    data object RecognizerUnavailable : VoiceState
}

class VoiceViewModel(
    private val transport: CommandTransport,
    private val parser: IntentParser,
    private val locale: Locale = Locale.getDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    fun beginListening() {
        _state.value = VoiceState.Listening
    }

    fun recognizerUnavailable() {
        _state.value = VoiceState.RecognizerUnavailable
    }

    fun onCancelled() {
        _state.value = VoiceState.Idle
    }

    fun onTranscript(transcript: String) {
        if (transcript.isBlank()) {
            _state.value = VoiceState.NotUnderstood
            return
        }

        viewModelScope.launch {
            _state.value = VoiceState.Sending
            when (
                val parsed = parser.parse(
                    transcript,
                    ParseContext(
                        nowEpochMs = System.currentTimeMillis(),
                        timeZoneId = TimeZone.getDefault().id,
                        languageTag = locale.toLanguageTag(),
                    ),
                )
            ) {
                is ParseOutcome.Parsed -> send(parsed)
                is ParseOutcome.NeedsClarification -> _state.value = VoiceState.NotUnderstood
                ParseOutcome.NotUnderstood -> _state.value = VoiceState.NotUnderstood
            }
        }
    }

    private suspend fun send(parsed: ParseOutcome.Parsed) {
        when (val outcome = transport.send(parsed.command)) {
            is TransportOutcome.Completed -> _state.value = when (val result = outcome.result) {
                is CommandResult.Success -> VoiceState.Launched(result.message.orEmpty())
                is CommandResult.Failure -> result.toVoiceState()
            }
            TransportOutcome.AgentUnreachable -> _state.value = VoiceState.AgentUnreachable
            TransportOutcome.TimedOut -> _state.value = VoiceState.TimedOut
            is TransportOutcome.DeliveryFailed ->
                _state.value = VoiceState.DeliveryFailed(outcome.detail)
        }
    }

    private fun CommandResult.Failure.toVoiceState(): VoiceState = when (reason) {
        FailureReason.APP_NOT_INSTALLED -> VoiceState.AppNotInstalled
        FailureReason.APP_NOT_LAUNCHABLE -> VoiceState.AppNotLaunchable
        FailureReason.APP_AMBIGUOUS -> VoiceState.Ambiguous(detail.orEmpty())
        FailureReason.LAUNCH_BLOCKED_NEEDS_USER_TAP -> VoiceState.NeedsUserTap(detail)
        else -> VoiceState.DeliveryFailed(detail ?: reason.name)
    }
}
