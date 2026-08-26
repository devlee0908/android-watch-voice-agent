package com.haesung.watchvoice.watch.domain

import com.haesung.watchvoice.protocol.WatchCommand

class RuleBasedIntentParser : IntentParser {

    override suspend fun parse(transcript: String, context: ParseContext): ParseOutcome {
        val text = transcript.trim()
        if (text.isEmpty()) return ParseOutcome.NotUnderstood

        parseKorean(text)?.let { return ParseOutcome.Parsed(WatchCommand.LaunchApp(it), 1f) }
        parseEnglish(text)?.let { return ParseOutcome.Parsed(WatchCommand.LaunchApp(it), 1f) }
        return ParseOutcome.NotUnderstood
    }

    private fun parseKorean(text: String): String? {
        val verb = KOREAN_VERB.find(text)?.let { match ->
            if (match.range.last != text.lastIndex) return null
            match
        } ?: return null
        var appName = text.substring(0, verb.range.first).trim()
        while (true) {
            val stripped = appName
                .removeSuffix("좀")
                .removeSuffix("앱")
                .removeSuffix("어플")
                .removeSuffix("애플리케이션")
                .trim()
                .removeSuffix("을")
                .removeSuffix("를")
                .trim()
            if (stripped == appName) break
            appName = stripped
        }
        return appName.normalizeAppName()
    }

    private fun parseEnglish(text: String): String? {
        val verb = ENGLISH_VERB.matchAt(text, 0) ?: return null
        var appName = text.substring(verb.range.last + 1).trim()
        if (appName.startsWith("the ", ignoreCase = true)) {
            appName = appName.substring(4).trim()
        }
        if (appName.endsWith(" app", ignoreCase = true)) {
            appName = appName.dropLast(4).trim()
        }
        return appName.normalizeAppName()
    }

    private fun String.normalizeAppName(): String? =
        lowercase()
            .filter { it.isLetterOrDigit() || it.isWhitespace() }
            .trim()
            .replace(Regex("\\s+"), " ")
            .takeIf { it.isNotEmpty() }

    companion object {
        private val KOREAN_VERB =
            Regex("(열어줘|열어|실행해줘|실행|켜줘|켜|시작해줘|시작)$")
        private val ENGLISH_VERB =
            Regex("(?i)(open|launch|start|run)\\b")
    }
}
