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

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun ConnectionScreenPreview() {
    ConnectionScreen(state = ConnectionState.Connected, onCheck = {})
}
