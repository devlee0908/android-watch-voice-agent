package com.haesung.watchvoice.phone.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.haesung.watchvoice.phone.domain.AppLauncher
import com.haesung.watchvoice.phone.domain.AppMatchResult
import com.haesung.watchvoice.phone.domain.AppNameMatcher
import com.haesung.watchvoice.phone.domain.InstalledApp
import com.haesung.watchvoice.phone.domain.LaunchOutcome
import timber.log.Timber

class AndroidAppLauncher(
    private val context: Context,
    private val matcher: AppNameMatcher = AppNameMatcher(),
) : AppLauncher {

    override suspend fun launch(appKey: String): LaunchOutcome {
        val installedApps = queryLaunchableApps()
        return when (val match = matcher.match(appKey, installedApps)) {
            AppMatchResult.NotFound -> LaunchOutcome.NotInstalled
            is AppMatchResult.Ambiguous -> LaunchOutcome.Ambiguous(match.candidateLabels)
            is AppMatchResult.Match -> launchMatch(match)
        }
    }

    private fun queryLaunchableApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager
            .queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                InstalledApp(
                    packageName = activityInfo.applicationInfo.packageName,
                    label = info.loadLabel(context.packageManager).toString(),
                )
            }
            .distinctBy { it.packageName }
    }

    private fun launchMatch(match: AppMatchResult.Match): LaunchOutcome {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(match.packageName)
            ?: return LaunchOutcome.NotLaunchable
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (!Settings.canDrawOverlays(context)) {
            postLaunchNotification(match.label, launchIntent)
            return LaunchOutcome.BlockedNeedsUserTap(match.label)
        }

        return try {
            context.startActivity(launchIntent)
            LaunchOutcome.Launched(match.label)
        } catch (exception: Exception) {
            Timber.w(exception, "Unable to launch %s from the background", match.packageName)
            postLaunchNotification(match.label, launchIntent)
            LaunchOutcome.BlockedNeedsUserTap(match.label)
        }
    }

    private fun postLaunchNotification(label: String, launchIntent: Intent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.launch_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            label.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.launch_notification_title))
            .setContentText(context.getString(R.string.launch_notification_text, label))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(label.hashCode(), notification)
        }.onFailure {
            Timber.w(it, "Unable to post launch notification")
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "app_launch"
    }
}
