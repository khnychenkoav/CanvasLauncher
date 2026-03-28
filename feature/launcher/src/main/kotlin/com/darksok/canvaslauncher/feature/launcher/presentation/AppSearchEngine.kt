package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import java.util.Locale

data class RankedSearchMatch(
    val packageName: String,
    val label: String,
    val score: Int,
)

object AppSearchEngine {

    fun rankByLabel(
        query: String,
        apps: List<CanvasApp>,
    ): List<RankedSearchMatch> {
        val normalizedQuery = query.normalizeForSearch()
        if (normalizedQuery.isBlank()) return emptyList()

        return apps.mapNotNull { app ->
            val score = score(
                query = normalizedQuery,
                label = app.label.normalizeForSearch(),
                packageName = app.packageName.normalizeForSearch(),
            )
            if (score < 0) return@mapNotNull null
            RankedSearchMatch(
                packageName = app.packageName,
                label = app.label,
                score = score,
            )
        }.sortedWith(
            compareByDescending<RankedSearchMatch> { it.score }
                .thenBy { it.label.length }
                .thenBy { it.label.lowercase(Locale.ROOT) },
        )
    }

    private fun score(
        query: String,
        label: String,
        packageName: String,
    ): Int {
        if (label == query) return 10_000
        if (packageName == query) return 9_800

        if (label.startsWith(query)) {
            return 9_000 - (label.length - query.length).coerceAtLeast(0)
        }
        if (packageName.startsWith(query)) {
            return 8_500 - (packageName.length - query.length).coerceAtLeast(0)
        }

        val words = label.split(' ').filter { it.isNotBlank() }
        val wordPrefixIndex = words.indexOfFirst { it.startsWith(query) }
        if (wordPrefixIndex >= 0) {
            val word = words[wordPrefixIndex]
            return 8_000 - wordPrefixIndex * 12 - (word.length - query.length).coerceAtLeast(0)
        }

        val containsIndex = label.indexOf(query)
        if (containsIndex >= 0) {
            return 7_000 - containsIndex * 8 - (label.length - query.length).coerceAtLeast(0)
        }

        val packageContainsIndex = packageName.indexOf(query)
        if (packageContainsIndex >= 0) {
            return 6_800 - packageContainsIndex * 6
        }

        val subsequencePenalty = subsequencePenalty(query, label)
            ?: subsequencePenalty(query, packageName)
            ?: return -1
        return 5_500 - subsequencePenalty
    }

    private fun subsequencePenalty(
        query: String,
        target: String,
    ): Int? {
        var targetCursor = 0
        var penalty = 0
        for (char in query) {
            val index = target.indexOf(char, startIndex = targetCursor)
            if (index < 0) return null
            penalty += (index - targetCursor).coerceAtLeast(0)
            targetCursor = index + 1
        }
        return penalty * 14 + (target.length - query.length).coerceAtLeast(0)
    }

    private fun String.normalizeForSearch(): String {
        return trim().lowercase(Locale.ROOT)
    }
}
