package com.haesung.watchvoice.phone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.haesung.watchvoice.phone.R

class MainActivity : ComponentActivity() {

    private var overlayGranted by mutableStateOf(false)
    private var notificationsGranted by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            MaterialTheme {
                Scaffold { insets ->
                    StatusScreen(
                        overlayGranted = overlayGranted,
                        notificationsGranted = notificationsGranted,
                        onOpenOverlaySettings = {
                            startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName"),
                                ),
                            )
                        },
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.padding(insets),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        overlayGranted = Settings.canDrawOverlays(this)
        notificationsGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun StatusScreen(
    overlayGranted: Boolean = false,
    notificationsGranted: Boolean = false,
    onOpenOverlaySettings: () -> Unit = {},
    onRequestNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.status_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.status_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(
                if (overlayGranted) R.string.overlay_granted else R.string.overlay_not_granted,
            ),
        )
        Button(onClick = onOpenOverlaySettings, enabled = !overlayGranted) {
            Text(text = stringResource(R.string.overlay_settings))
        }
        Text(text = stringResource(R.string.overlay_explanation))
        Text(
            text = stringResource(
                if (notificationsGranted) {
                    R.string.notifications_granted
                } else {
                    R.string.notifications_not_granted
                },
            ),
        )
        Button(onClick = onRequestNotifications, enabled = !notificationsGranted) {
            Text(text = stringResource(R.string.notification_settings))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusScreenPreview() {
    MaterialTheme { StatusScreen() }
}
