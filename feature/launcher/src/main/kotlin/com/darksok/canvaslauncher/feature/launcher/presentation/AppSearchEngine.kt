package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

data class RankedSearchMatch(
    val packageName: String,
    val label: String,
    val score: Int,
)

data class SearchIndex internal constructor(
    internal val entries: List<IndexedApp>,
    internal val entriesSortedByLabel: List<IndexedApp>,
)

internal data class IndexedApp(
    val packageName: String,
    val label: String,
    val labelSearchText: SearchText,
    val packageSearchText: SearchText,
)

internal data class SearchVariant(
    val value: String,
    val penalty: Int,
)

internal data class SearchText(
    val variants: List<SearchVariant>,
    val tokens: Set<String>,
    val expandedTokens: Set<String>,
)

object AppSearchEngine {

    fun buildIndex(apps: List<CanvasApp>): SearchIndex {
        val entries = apps.map { app ->
            IndexedApp(
                packageName = app.packageName,
                label = app.label,
                labelSearchText = buildSearchText(app.label),
                packageSearchText = buildSearchText(app.packageName),
            )
        }
        val sortedEntries = entries.sortedBy { entry -> entry.label.lowercase(Locale.ROOT) }
        return SearchIndex(
            entries = entries,
            entriesSortedByLabel = sortedEntries,
        )
    }

    fun rankByLabel(
        query: String,
        apps: List<CanvasApp>,
    ): List<RankedSearchMatch> {
        return rankByLabel(
            query = query,
            searchIndex = buildIndex(apps),
        )
    }

    fun rankByLabel(
        query: String,
        searchIndex: SearchIndex,
    ): List<RankedSearchMatch> {
        val queryText = buildSearchText(query)
        if (queryText.variants.isEmpty()) return emptyList()

        return searchIndex.entries.mapNotNull { app ->
            val score = score(
                query = queryText,
                label = app.labelSearchText,
                packageName = app.packageSearchText,
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
        query: SearchText,
        label: SearchText,
        packageName: SearchText,
    ): Int {
        val labelScore = bestVariantScore(
            query = query,
            target = label,
            targetPenalty = 0,
        )
        val packageScore = bestVariantScore(
            query = query,
            target = packageName,
            targetPenalty = 150,
        )
        val semanticLabelScore = semanticScore(
            queryTokens = query.expandedTokens,
            targetTokens = label.expandedTokens,
            base = 8_900,
        )
        val semanticPackageScore = semanticScore(
            queryTokens = query.expandedTokens,
            targetTokens = packageName.expandedTokens,
            base = 8_500,
        )

        return maxOf(
            labelScore,
            packageScore,
            semanticLabelScore,
            semanticPackageScore,
        )
    }

    private fun bestVariantScore(
        query: SearchText,
        target: SearchText,
        targetPenalty: Int,
    ): Int {
        var best = -1
        for (queryVariant in query.variants) {
            for (targetVariant in target.variants) {
                val baseScore = baseScore(
                    query = queryVariant.value,
                    target = targetVariant.value,
                    targetTokens = target.tokens,
                )
                if (baseScore < 0) continue
                val score = baseScore - queryVariant.penalty - targetVariant.penalty - targetPenalty
                if (score > best) best = score
            }
        }
        return best
    }

    private fun baseScore(
        query: String,
        target: String,
        targetTokens: Set<String>,
    ): Int {
        if (target == query) return 10_000

        if (target.startsWith(query)) {
            return 9_250 - (target.length - query.length).coerceAtLeast(0)
        }

        val words = target.split(' ').filter { it.isNotBlank() }
        val wordPrefixIndex = words.indexOfFirst { it.startsWith(query) }
        if (wordPrefixIndex >= 0) {
            val word = words[wordPrefixIndex]
            return 8_850 - wordPrefixIndex * 12 - (word.length - query.length).coerceAtLeast(0)
        }

        val containsIndex = target.indexOf(query)
        if (containsIndex >= 0) {
            return 8_150 - containsIndex * 8 - (target.length - query.length).coerceAtLeast(0)
        }

        val subsequencePenalty = subsequencePenalty(query, target)
        if (subsequencePenalty != null) {
            return 7_250 - subsequencePenalty
        }

        val fuzzyPenalty = fuzzyPenalty(
            query = query,
            target = target,
            targetTokens = targetTokens,
        ) ?: return -1
        return 6_300 - fuzzyPenalty
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

    private fun fuzzyPenalty(
        query: String,
        target: String,
        targetTokens: Set<String>,
    ): Int? {
        val maxDistance = allowedDistance(query.length)
        val candidates = LinkedHashSet<String>()
        candidates += target
        candidates += targetTokens

        var bestPenalty: Int? = null
        for (candidate in candidates) {
            val lengthDelta = abs(candidate.length - query.length)
            if (lengthDelta > maxDistance + 2) continue

            val distance = levenshteinDistanceWithin(
                left = query,
                right = candidate,
                maxDistance = maxDistance,
            ) ?: continue

            val penalty = distance * 190 + lengthDelta * 34
            if (bestPenalty == null || penalty < bestPenalty) {
                bestPenalty = penalty
            }
        }
        return bestPenalty
    }

    private fun allowedDistance(length: Int): Int {
        return when {
            length <= 4 -> 1
            length <= 7 -> 2
            length <= 12 -> 3
            else -> 4
        }
    }

    private fun levenshteinDistanceWithin(
        left: String,
        right: String,
        maxDistance: Int,
    ): Int? {
        if (left == right) return 0
        if (abs(left.length - right.length) > maxDistance) return null
        if (left.isEmpty()) return right.length.takeIf { it <= maxDistance }
        if (right.isEmpty()) return left.length.takeIf { it <= maxDistance }

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)

        for (leftIndex in 1..left.length) {
            current[0] = leftIndex
            var rowMin = current[0]
            val leftChar = left[leftIndex - 1]

            for (rightIndex in 1..right.length) {
                val substitutionCost = if (leftChar == right[rightIndex - 1]) 0 else 1
                val substitution = previous[rightIndex - 1] + substitutionCost
                val insertion = current[rightIndex - 1] + 1
                val deletion = previous[rightIndex] + 1
                val value = minOf(substitution, insertion, deletion)
                current[rightIndex] = value
                if (value < rowMin) rowMin = value
            }

            if (rowMin > maxDistance) return null
            val swap = previous
            previous = current
            current = swap
        }

        return previous[right.length].takeIf { it <= maxDistance }
    }

    private fun semanticScore(
        queryTokens: Set<String>,
        targetTokens: Set<String>,
        base: Int,
    ): Int {
        if (queryTokens.isEmpty() || targetTokens.isEmpty()) return -1
        val intersection = queryTokens.intersect(targetTokens)
        if (intersection.isEmpty()) return -1

        val shortest = intersection.minOf { token -> token.length }
        return base - shortest.coerceAtMost(10)
    }

    private fun buildSearchText(raw: String): SearchText {
        val normalized = raw.normalizeForSearch()
        if (normalized.isBlank()) {
            return SearchText(
                variants = emptyList(),
                tokens = emptySet(),
                expandedTokens = emptySet(),
            )
        }

        val variantsByPenalty = LinkedHashMap<String, Int>()
        fun addVariant(value: String, penalty: Int) {
            if (value.isBlank()) return
            val currentPenalty = variantsByPenalty[value]
            if (currentPenalty == null || penalty < currentPenalty) {
                variantsByPenalty[value] = penalty
            }
        }

        val keyboardSwap = swapKeyboardLayout(normalized)

        addVariant(normalized, penalty = 0)
        addVariant(transliterateCyrillicToLatin(normalized), penalty = 90)
        addVariant(transliterateLatinToCyrillic(normalized), penalty = 110)
        addVariant(keyboardSwap, penalty = 130)
        addVariant(transliterateLatinToCyrillic(keyboardSwap), penalty = 180)
        addVariant(transliterateCyrillicToLatin(keyboardSwap), penalty = 180)

        val variants = variantsByPenalty.entries
            .sortedBy { (_, penalty) -> penalty }
            .map { (value, penalty) ->
                SearchVariant(
                    value = value,
                    penalty = penalty,
                )
            }

        val tokens = variants.asSequence()
            .flatMap { variant ->
                tokenize(variant.value).asSequence()
            }
            .toCollection(LinkedHashSet())

        val expandedTokens = tokens.toMutableSet()
        tokens.forEach { token ->
            TOKEN_SYNONYMS[token]?.let { aliases ->
                expandedTokens += aliases
            }
        }

        return SearchText(
            variants = variants,
            tokens = tokens,
            expandedTokens = expandedTokens,
        )
    }

    private fun tokenize(value: String): Set<String> {
        return value.split(' ')
            .asSequence()
            .map(String::trim)
            .filter { token ->
                token.length > 1 && token !in STOP_WORD_TOKENS
            }
            .toCollection(LinkedHashSet())
    }

    private fun transliterateCyrillicToLatin(value: String): String {
        return buildString(value.length + 8) {
            value.forEach { char ->
                append(CYRILLIC_TO_LATIN[char] ?: char)
            }
        }
    }

    private fun transliterateLatinToCyrillic(value: String): String {
        val builder = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (!char.isLetter()) {
                builder.append(char)
                index += 1
                continue
            }

            var matched = false
            for ((latin, cyrillic) in LATIN_TO_CYRILLIC_MULTI_CHAR) {
                if (value.startsWith(latin, startIndex = index)) {
                    builder.append(cyrillic)
                    index += latin.length
                    matched = true
                    break
                }
            }
            if (matched) continue

            builder.append(LATIN_TO_CYRILLIC_SINGLE_CHAR[char] ?: char.toString())
            index += 1
        }
        return builder.toString()
    }

    private fun swapKeyboardLayout(value: String): String {
        return buildString(value.length) {
            value.forEach { char ->
                append(KEYBOARD_LAYOUT_SWAP[char] ?: char)
            }
        }
    }

    private fun String.normalizeForSearch(): String {
        val lowered = trim()
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .replace("ß", "ss")

        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val noDiacritics = COMBINING_MARKS_REGEX.replace(decomposed, "")
        val wordsOnly = NON_WORD_REGEX.replace(noDiacritics, " ")
        return MULTI_SPACE_REGEX.replace(wordsOnly, " ").trim()
    }

    private val COMBINING_MARKS_REGEX = Regex("\\p{Mn}+")
    private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{Nd}]+")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    private val STOP_WORD_TOKENS = setOf(
        "app",
        "apps",
        "application",
        "launcher",
        "com",
        "android",
        "google",
        "the",
    )

    private val CYRILLIC_TO_LATIN = mapOf(
        'а' to "a",
        'б' to "b",
        'в' to "v",
        'г' to "g",
        'д' to "d",
        'е' to "e",
        'ё' to "e",
        'ж' to "zh",
        'з' to "z",
        'и' to "i",
        'й' to "y",
        'к' to "k",
        'л' to "l",
        'м' to "m",
        'н' to "n",
        'о' to "o",
        'п' to "p",
        'р' to "r",
        'с' to "s",
        'т' to "t",
        'у' to "u",
        'ф' to "f",
        'х' to "kh",
        'ц' to "ts",
        'ч' to "ch",
        'ш' to "sh",
        'щ' to "shch",
        'ы' to "y",
        'э' to "e",
        'ю' to "yu",
        'я' to "ya",
        'ь' to "",
        'ъ' to "",
    )

    private val LATIN_TO_CYRILLIC_MULTI_CHAR = listOf(
        "shch" to "щ",
        "yo" to "е",
        "yu" to "ю",
        "ya" to "я",
        "zh" to "ж",
        "kh" to "х",
        "ts" to "ц",
        "ch" to "ч",
        "sh" to "ш",
        "ye" to "е",
    )

    private val LATIN_TO_CYRILLIC_SINGLE_CHAR = mapOf(
        'a' to "а",
        'b' to "б",
        'c' to "к",
        'd' to "д",
        'e' to "е",
        'f' to "ф",
        'g' to "г",
        'h' to "х",
        'i' to "и",
        'j' to "й",
        'k' to "к",
        'l' to "л",
        'm' to "м",
        'n' to "н",
        'o' to "о",
        'p' to "п",
        'q' to "к",
        'r' to "р",
        's' to "с",
        't' to "т",
        'u' to "у",
        'v' to "в",
        'w' to "в",
        'x' to "кс",
        'y' to "й",
        'z' to "з",
    )

    private val KEYBOARD_LAYOUT_SWAP = run {
        val english = "`qwertyuiop[]asdfghjkl;'zxcvbnm,./"
        val russian = "ёйцукенгшщзхъфывапролджэячсмитьбю."
        buildMap<Char, Char> {
            english.zip(russian).forEach { (en, ru) ->
                put(en, ru)
                put(ru, en)
            }
        }
    }

    private val TOKEN_SYNONYMS: Map<String, Set<String>> = run {
        val groups = listOf(
            setOf(
                "call",
                "phone",
                "dialer",
                "telephone",
                "caller",
                "звонок",
                "звонки",
                "звонить",
                "телефон",
                "анруф",
                "anruf",
                "appel",
                "llamada",
                "telefone",
                "chamada",
                "ligacao",
                "ligar",
                "telefono",
                "zvonok",
                "telefon",
            ),
            setOf(
                "message",
                "messages",
                "sms",
                "chat",
                "messenger",
                "сообщение",
                "сообщения",
                "смс",
                "чат",
                "nachricht",
                "mensaje",
                "mensagem",
                "message",
            ),
            setOf(
                "camera",
                "cam",
                "photo",
                "photos",
                "camera",
                "камера",
                "фото",
                "kamera",
                "foto",
                "appareil",
            ),
            setOf(
                "browser",
                "web",
                "internet",
                "chrome",
                "браузер",
                "интернет",
                "navegador",
                "navigateur",
                "navegador",
                "browser",
            ),
            setOf(
                "settings",
                "config",
                "preferences",
                "настройки",
                "параметры",
                "einstellungen",
                "ajustes",
                "parametres",
                "configuracoes",
            ),
        )

        val normalizedGroups = groups.map { group ->
            group.asSequence()
                .map { token -> token.normalizeForSearch() }
                .filter { token -> token.isNotBlank() }
                .flatMap { token ->
                    sequenceOf(
                        token,
                        transliterateCyrillicToLatin(token),
                        transliterateLatinToCyrillic(token),
                    )
                }
                .flatMap { token -> tokenize(token).asSequence() }
                .toCollection(LinkedHashSet())
        }

        buildMap<String, Set<String>> {
            normalizedGroups.forEach { group ->
                group.forEach { token ->
                    val aliases = get(token)?.toMutableSet() ?: LinkedHashSet()
                    aliases += group
                    put(token, aliases)
                }
            }
        }
    }
}
