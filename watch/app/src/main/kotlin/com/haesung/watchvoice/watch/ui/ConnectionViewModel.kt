package com.haesung.watchvoice.watch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.WatchCommand
import com.haesung.watchvoice.watch.domain.CommandTransport
import com.haesung.watchvoice.watch.domain.TransportOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Phase 0 screen state: proves the watch can reach the phone companion and get an answer back. */
sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Checking : ConnectionState
    data object Connected : ConnectionState
    data object CompanionUnreachable : ConnectionState
    data object TimedOut : ConnectionState
    data class Failed(val detail: String?) : ConnectionState
}

class ConnectionViewModel(private val transport: CommandTransport) : ViewModel() {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun check() {
        if (_state.value == ConnectionState.Checking) return
        _state.value = ConnectionState.Checking
        viewModelScope.launch {
            _state.value = when (val outcome = transport.send(WatchCommand.Ping)) {
                is TransportOutcome.Completed -> when (val result = outcome.result) {
                    is CommandResult.Success -> ConnectionState.Connected
                    is CommandResult.Failure -> ConnectionState.Failed(result.reason.name)
                }
                TransportOutcome.CompanionUnreachable -> ConnectionState.CompanionUnreachable
                TransportOutcome.TimedOut -> ConnectionState.TimedOut
                is TransportOutcome.DeliveryFailed -> ConnectionState.Failed(outcome.detail)
            }
        }
    }
}
