package com.haesung.watchvoice.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProtocolRoundTripTest {

    private fun envelope(command: WatchCommand) = CommandEnvelope(
        commandId = "11111111-2222-3333-4444-555555555555",
        sentAtEpochMs = 1_787_000_000_000,
        command = command,
    )

    @Test
    fun `ping round trips`() {
        val original = envelope(WatchCommand.Ping)
        assertThat(decodeEnvelope(original.encode())).isEqualTo(original)
    }

    @Test
    fun `launch app round trips`() {
        val original = envelope(WatchCommand.LaunchApp(appKey = "spotify"))
        assertThat(decodeEnvelope(original.encode())).isEqualTo(original)
    }

    @Test
    fun `calendar draft round trips with optional fields`() {
        val original = envelope(
            WatchCommand.CreateCalendarEvent(
                CalendarEventDraft(
                    title = "팀 회의",
                    startEpochMs = 1_787_000_000_000,
                    endEpochMs = 1_787_003_600_000,
                    timeZoneId = "Asia/Seoul",
                    location = "P103",
                ),
            ),
        )
        assertThat(decodeEnvelope(original.encode())).isEqualTo(original)
    }

    @Test
    fun `results round trip`() {
        val success: CommandResult = CommandResult.Success("abc", message = "Spotify")
        val failure: CommandResult = CommandResult.Failure("abc", FailureReason.APP_NOT_INSTALLED)

        assertThat(decodeResult(success.encode())).isEqualTo(success)
        assertThat(decodeResult(failure.encode())).isEqualTo(failure)
    }

    @Test
    fun `unknown fields from a newer peer are ignored`() {
        val json = """
            {"commandId":"abc","protocolVersion":1,"sentAtEpochMs":1,
             "command":{"type":"com.haesung.watchvoice.protocol.WatchCommand.LaunchApp",
                        "appKey":"spotify","unknownFuture":"x"},
             "unknownTopLevel":true}
        """.trimIndent()

        val decoded = decodeEnvelope(json.encodeToByteArray())

        assertThat(decoded.command).isEqualTo(WatchCommand.LaunchApp("spotify"))
    }
}
