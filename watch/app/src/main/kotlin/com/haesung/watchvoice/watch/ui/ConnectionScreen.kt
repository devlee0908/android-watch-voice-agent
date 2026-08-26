package com.haesung.watchvoice.watch.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.haesung.watchvoice.watch.R

@Composable
fun ConnectionScreen(
    state: ConnectionState,
    onCheck: () -> Unit,
    voiceState: VoiceState = VoiceState.Idle,
    onListen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    MaterialTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(state.messageRes()), textAlign = TextAlign.Center)
            Button(onClick = onCheck) {
                Text(text = stringResource(R.string.connection_check))
            }
            Text(text = voiceState.message(), textAlign = TextAlign.Center)
            Button(onClick = onListen) {
                Text(text = stringResource(R.string.voice_input))
            }
        }
    }
}

private fun ConnectionState.messageRes(): Int = when (this) {
    ConnectionState.Idle -> R.string.app_name
    ConnectionState.Checking -> R.string.checking
    ConnectionState.Connected -> R.string.connected
    ConnectionState.AgentUnreachable -> R.string.agent_unreachable
    ConnectionState.TimedOut -> R.string.timed_out
    is ConnectionState.Failed -> R.string.delivery_failed
}

@Composable
private fun VoiceState.message(): String = when (this) {
    VoiceState.Idle -> stringResource(R.string.voice_ready)
    VoiceState.Listening -> stringResource(R.string.voice_listening)
    VoiceState.Sending -> stringResource(R.string.voice_sending)
    is VoiceState.Launched -> stringResource(R.string.voice_launched, label)
    VoiceState.NotUnderstood -> stringResource(R.string.voice_not_understood)
    VoiceState.AppNotInstalled -> stringResource(R.string.voice_app_not_installed)
    VoiceState.AppNotLaunchable -> stringResource(R.string.voice_app_not_launchable)
    is VoiceState.Ambiguous -> stringResource(R.string.voice_ambiguous, candidates)
    is VoiceState.NeedsUserTap -> stringResource(
        R.string.voice_needs_user_tap,
        detail.orEmpty(),
    )
    VoiceState.AgentUnreachable -> stringResource(R.string.agent_unreachable)
    VoiceState.TimedOut -> stringResource(R.string.timed_out)
    is VoiceState.DeliveryFailed -> stringResource(R.string.delivery_failed)
    VoiceState.RecognizerUnavailable -> stringResource(R.string.voice_recognizer_unavailable)
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun ConnectionScreenPreview() {
    ConnectionScreen(state = ConnectionState.Connected, onCheck = {})
}
