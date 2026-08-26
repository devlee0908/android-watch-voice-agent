package com.haesung.watchvoice.watch.ui

import com.google.common.truth.Truth.assertThat
import com.haesung.watchvoice.protocol.CommandResult
import com.haesung.watchvoice.protocol.FailureReason
import com.haesung.watchvoice.protocol.WatchCommand
import com.haesung.watchvoice.watch.domain.CommandTransport
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
class ConnectionViewModelTest {

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

    @Test
    fun `check sends a ping and reports connected on success`() = runTest(dispatcher) {
        val transport = FakeTransport(
            TransportOutcome.Completed(CommandResult.Success("id", message = "pong")),
        )
        val viewModel = ConnectionViewModel(transport)

        viewModel.check()
        testScheduler.advanceUntilIdle()

        assertThat(transport.sent).isEqualTo(WatchCommand.Ping)
        assertThat(viewModel.state.value).isEqualTo(ConnectionState.Connected)
    }

    @Test
    fun `an unreachable agent is distinct from a failure`() = runTest(dispatcher) {
        val viewModel = ConnectionViewModel(FakeTransport(TransportOutcome.AgentUnreachable))

        viewModel.check()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ConnectionState.AgentUnreachable)
    }

    @Test
    fun `a failing result surfaces its reason`() = runTest(dispatcher) {
        val transport = FakeTransport(
            TransportOutcome.Completed(
                CommandResult.Failure("id", FailureReason.UNSUPPORTED_COMMAND),
            ),
        )
        val viewModel = ConnectionViewModel(transport)

        viewModel.check()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.state.value)
            .isEqualTo(ConnectionState.Failed(FailureReason.UNSUPPORTED_COMMAND.name))
    }
}
