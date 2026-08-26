package com.haesung.watchvoice.phone.domain

data class InstalledApp(
    val packageName: String,
    val label: String,
)

sealed interface AppMatchResult {
    data class Match(
        val packageName: String,
        val label: String,
    ) : AppMatchResult

    data class Ambiguous(val candidateLabels: List<String>) : AppMatchResult

    data object NotFound : AppMatchResult
}

class AppNameMatcher(
    private val aliases: Map<String, String> = DEFAULT_ALIASES,
    private val fuzzyThreshold: Double = FUZZY_THRESHOLD,
    private val ambiguityMargin: Double = AMBIGUITY_MARGIN,
) {
    private val normalizedAliases = aliases.entries.associate { normalize(it.key) to it.value }

    fun match(spokenName: String, installedApps: List<InstalledApp>): AppMatchResult {
        val normalizedName = normalize(spokenName)
        if (normalizedName.isEmpty()) return AppMatchResult.NotFound

        val installedByPackage = installedApps.associateBy { it.packageName }
        normalizedAliases[normalizedName]?.let { packageName ->
            installedByPackage[packageName]?.let { app ->
                return AppMatchResult.Match(app.packageName, app.label)
            }
        }

        val normalizedApps = installedApps.map { app ->
            NormalizedApp(app, normalize(app.label))
        }
        val exactMatches = normalizedApps.filter { it.normalizedLabel == normalizedName }
        if (exactMatches.size == 1) {
            return exactMatches.single().toMatch()
        }
        if (exactMatches.size > 1) {
            return AppMatchResult.Ambiguous(exactMatches.map { it.app.label }.distinct())
        }

        val partialMatches = normalizedApps.filter {
            it.normalizedLabel.startsWith(normalizedName) ||
                it.normalizedLabel.contains(normalizedName)
        }
        if (partialMatches.size == 1) {
            return partialMatches.single().toMatch()
        }
        if (partialMatches.size > 1) {
            return AppMatchResult.Ambiguous(partialMatches.map { it.app.label }.distinct())
        }

        val fuzzyMatches = normalizedApps
            .map { it to similarity(normalizedName, it.normalizedLabel) }
            .filter { (_, score) -> score >= fuzzyThreshold }
            .sortedByDescending { (_, score) -> score }
        if (fuzzyMatches.isEmpty()) return AppMatchResult.NotFound

        val bestScore = fuzzyMatches.first().second
        val candidates = fuzzyMatches
            .takeWhile { (_, score) -> bestScore - score <= ambiguityMargin }
            .map { it.first.app }
        return if (candidates.size > 1) {
            AppMatchResult.Ambiguous(candidates.map { it.label }.distinct())
        } else {
            candidates.single().let { AppMatchResult.Match(it.packageName, it.label) }
        }
    }

    private fun NormalizedApp.toMatch() = AppMatchResult.Match(app.packageName, app.label)

    private data class NormalizedApp(
        val app: InstalledApp,
        val normalizedLabel: String,
    )

    companion object {
        const val FUZZY_THRESHOLD = 0.7
        const val AMBIGUITY_MARGIN = 0.05

        val DEFAULT_ALIASES: Map<String, String> = mapOf(
            "kakaotalk" to "com.kakao.talk",
            "카카오톡" to "com.kakao.talk",
            "spotify" to "com.spotify.music",
            "스포티파이" to "com.spotify.music",
            "youtube" to "com.google.android.youtube",
            "유튜브" to "com.google.android.youtube",
            "instagram" to "com.instagram.android",
            "인스타그램" to "com.instagram.android",
            "facebook" to "com.facebook.katana",
            "페이스북" to "com.facebook.katana",
            "telegram" to "org.telegram.messenger",
            "텔레그램" to "org.telegram.messenger",
            "chrome" to "com.android.chrome",
            "크롬" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "지메일" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "지도" to "com.google.android.apps.maps",
        )

        fun normalize(value: String): String =
            value.lowercase().filterNot { it.isWhitespace() || it.isLetterOrDigit().not() }

        private fun similarity(left: String, right: String): Double {
            val longestLength = maxOf(left.length, right.length)
            if (longestLength == 0) return 1.0
            return 1.0 - levenshteinDistance(left, right).toDouble() / longestLength
        }

        private fun levenshteinDistance(left: String, right: String): Int {
            var previous = IntArray(right.length + 1) { it }
            for (leftIndex in left.indices) {
                val current = IntArray(right.length + 1)
                current[0] = leftIndex + 1
                for (rightIndex in right.indices) {
                    current[rightIndex + 1] = minOf(
                        current[rightIndex] + 1,
                        previous[rightIndex + 1] + 1,
                        previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1,
                    )
                }
                previous = current
            }
            return previous[right.length]
        }
    }
}
