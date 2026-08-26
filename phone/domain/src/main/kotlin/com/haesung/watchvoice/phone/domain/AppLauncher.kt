package com.haesung.watchvoice.phone.domain

/**
 * Resolves a logical app key (`"spotify"`) against the apps installed on this phone and starts it.
 *
 * Only the phone can do this resolution, which is why the watch never sends a package name,
 * component, or intent — a message from the watch cannot name an arbitrary target.
 */
interface AppLauncher {
    suspend fun launch(appKey: String): LaunchOutcome
}

sealed interface LaunchOutcome {
    data class Launched(val label: String) : LaunchOutcome

    data object NotInstalled : LaunchOutcome

    /** Installed but has no launchable activity (background-only or widget-only apps). */
    data object NotLaunchable : LaunchOutcome

    /**
     * Resolved, but the background activity launch was refused — the user has not granted
     * "display over other apps", so the companion must fall back to a notification.
     */
    data class BlockedNeedsUserTap(val label: String) : LaunchOutcome
}
