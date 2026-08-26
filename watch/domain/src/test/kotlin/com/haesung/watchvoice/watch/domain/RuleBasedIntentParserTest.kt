package com.haesung.watchvoice.watch.domain

import com.google.common.truth.Truth.assertThat
import com.haesung.watchvoice.protocol.WatchCommand
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RuleBasedIntentParserTest {

    private val parser = RuleBasedIntentParser()
    private val context = ParseContext(0, "Asia/Seoul", "ko-KR")

    @Test
    fun `korean command strips filler and object particle`() = runTest {
        assertThat(parser.parse("스포티파이 좀 열어줘", context))
            .isEqualTo(ParseOutcome.Parsed(WatchCommand.LaunchApp("스포티파이"), 1f))
        assertThat(parser.parse("카카오톡을 실행해줘", context))
            .isEqualTo(ParseOutcome.Parsed(WatchCommand.LaunchApp("카카오톡"), 1f))
    }

    @Test
    fun `korean app filler is stripped`() = runTest {
        assertThat(parser.parse("유튜브 앱 켜", context))
            .isEqualTo(ParseOutcome.Parsed(WatchCommand.LaunchApp("유튜브"), 1f))
    }

    @Test
    fun `english command strips the and app`() = runTest {
        assertThat(parser.parse("open spotify", context))
            .isEqualTo(ParseOutcome.Parsed(WatchCommand.LaunchApp("spotify"), 1f))
        assertThat(parser.parse("launch the spotify app", context))
            .isEqualTo(ParseOutcome.Parsed(WatchCommand.LaunchApp("spotify"), 1f))
    }

    @Test
    fun `blank and non-command transcripts are not understood`() = runTest {
        assertThat(parser.parse("  ", context)).isEqualTo(ParseOutcome.NotUnderstood)
        assertThat(parser.parse("spotify please", context)).isEqualTo(ParseOutcome.NotUnderstood)
        assertThat(parser.parse("열어줘", context)).isEqualTo(ParseOutcome.NotUnderstood)
        assertThat(parser.parse("open", context)).isEqualTo(ParseOutcome.NotUnderstood)
    }
}
