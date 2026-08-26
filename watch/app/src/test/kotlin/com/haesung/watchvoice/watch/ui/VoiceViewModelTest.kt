package com.haesung.watchvoice.watch.ui

import com.google.common.truth.Truth.assertThat
import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.FailureReason
import com.haesung.watchvoice.protocol.WatchCommand
import com.haesung.watchvoice.watch.domain.CommandTransport
import com.haesung.watchvoice.watch.domain.IntentParser
import com.haesung.watchvoice.watch.domain.ParseContext
import com.haesung.watchvoice.watch.domain.ParseOutcome
import com.haesung.watchvoice.watch.domain.TransportOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private class FakeTransport(private val outcome: TransportOutcome) : CommandTransport {
        var sent: WatchCommand? = null

        override suspend fun send(command: WatchCommand): TransportOutcome {
            sent = command
            return outcome
        }
    }

    private class FakeParser(private val outcome: ParseOutcome) : IntentParser {
        override suspend fun parse(
            transcript: String,
            context: ParseContext,
        ) = outcome
    }

    @Test
    fun `parsed transcript is sent and label is shown`() = runTest(dispatcher) {
        val transport = FakeTransport(
            TransportOutcome.Completed(CommandResult.Success("id", message = "Spotify")),
        )
        val viewModel = VoiceViewModel(
            transport,
            FakeParser(
                    ParseOutcome.Parsed(
                    WatchCommand.LaunchApp("spotify"),
                    1f,
                ),
            ),
        )

        viewModel.onTranscript("open spotify")
        testScheduler.advanceUntilIdle()

        assertThat(transport.sent).isEqualTo(WatchCommand.LaunchApp("spotify"))
        assertThat(viewModel.state.value).isEqualTo(VoiceState.Launched("Spotify"))
    }

    @Test
    fun `failure reasons map to distinct voice states`() = runTest(dispatcher) {
        val viewModel = VoiceViewModel(
            FakeTransport(
                TransportOutcome.Completed(
                    CommandResult.Failure("id", FailureReason.APP_AMBIGUOUS, "Notes, Noted"),
                ),
            ),
            FakeParser(
                ParseOutcome.Parsed(
                    WatchCommand.LaunchApp("note"),
                    1f,
                ),
            ),
        )

        viewModel.onTranscript("open note")
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(VoiceState.Ambiguous("Notes, Noted"))
    }

    @Test
    fun `unparsed transcript does not send`() = runTest(dispatcher) {
        val transport = FakeTransport(TransportOutcome.AgentUnreachable)
        val viewModel = VoiceViewModel(
            transport,
            FakeParser(ParseOutcome.NotUnderstood),
        )

        viewModel.onTranscript("what time is it")
        testScheduler.advanceUntilIdle()

        assertThat(transport.sent).isNull()
        assertThat(viewModel.state.value).isEqualTo(VoiceState.NotUnderstood)
    }

    @Test
    fun `cancelled recognition returns to idle`() = runTest(dispatcher) {
        val viewModel = VoiceViewModel(
            FakeTransport(TransportOutcome.AgentUnreachable),
            FakeParser(ParseOutcome.NotUnderstood),
        )

        viewModel.beginListening()
        viewModel.onCancelled()

        assertThat(viewModel.state.value).isEqualTo(VoiceState.Idle)
    }
}
