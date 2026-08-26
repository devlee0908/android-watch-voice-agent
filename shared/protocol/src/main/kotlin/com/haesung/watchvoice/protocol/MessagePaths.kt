package com.haesung.watchvoice.protocol

/** Wearable Data Layer paths shared by both apps. */
object MessagePaths {
    const val COMMAND = "/watchvoice/command"
    const val RESULT = "/watchvoice/result"

    /** DataClient item carrying the phone's launchable-app list and permission state. */
    const val PHONE_STATE = "/watchvoice/phone_state"

    /** Capability advertised by the phone companion so the watch can detect it. */
    const val COMPANION_CAPABILITY = "watchvoice_companion"
}
