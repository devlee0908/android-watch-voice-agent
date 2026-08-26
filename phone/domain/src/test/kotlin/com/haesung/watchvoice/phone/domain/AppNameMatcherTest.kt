package com.haesung.watchvoice.phone.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppNameMatcherTest {

    private val apps = listOf(
        InstalledApp("com.kakao.talk", "KakaoTalk"),
        InstalledApp("com.spotify.music", "Spotify"),
        InstalledApp("com.example.notes", "Notes"),
        InstalledApp("com.example.notepad", "Notepad"),
    )

    @Test
    fun `installed alias wins`() {
        assertThat(AppNameMatcher().match("카카오톡", apps))
            .isEqualTo(AppMatchResult.Match("com.kakao.talk", "KakaoTalk"))
    }

    @Test
    fun `alias not installed falls through to labels`() {
        val result = AppNameMatcher().match(
            "spotify",
            listOf(InstalledApp("com.example.spotify", "Spotify")),
        )
        assertThat(result).isEqualTo(AppMatchResult.Match("com.example.spotify", "Spotify"))
    }

    @Test
    fun `exact label matches`() {
        assertThat(AppNameMatcher().match("notes", apps))
            .isEqualTo(AppMatchResult.Match("com.example.notes", "Notes"))
    }

    @Test
    fun `partial label matches`() {
        assertThat(AppNameMatcher().match("note", apps))
            .isEqualTo(AppMatchResult.Ambiguous(listOf("Notes", "Notepad")))
    }

    @Test
    fun `fuzzy label matches`() {
        assertThat(AppNameMatcher().match("spotfy", apps))
            .isEqualTo(AppMatchResult.Match("com.spotify.music", "Spotify"))
    }

    @Test
    fun `close fuzzy matches are ambiguous`() {
        val result = AppNameMatcher().match(
            "noteb",
            listOf(
                InstalledApp("com.example.notes", "Notes"),
                InstalledApp("com.example.noted", "Noted"),
            ),
        )
        assertThat(result).isEqualTo(AppMatchResult.Ambiguous(listOf("Notes", "Noted")))
    }

    @Test
    fun `unknown app is not found`() {
        assertThat(AppNameMatcher().match("calendar", apps))
            .isEqualTo(AppMatchResult.NotFound)
    }
}
