package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.InstalledApp
import java.util.Locale

data class SmartLayoutGroup(
    val id: String,
    val title: String,
    val colorArgb: Int,
    val sortOrder: Int,
    val apps: List<InstalledApp>,
)

object SmartLayoutGrouping {

    fun group(apps: List<InstalledApp>): List<SmartLayoutGroup> {
        val prepared = apps
            .distinctBy { app -> app.packageName }
            .sortedBy { app -> app.label.lowercase(Locale.ROOT) }
            .map { app -> PreparedApp.from(app) }
        if (prepared.isEmpty()) return emptyList()

        val semanticDescriptorByPackage = prepared.associate { record ->
            record.app.packageName to semanticDescriptor(record)
        }

        val vendorCounts = prepared
            .asSequence()
            .filter { record -> semanticDescriptorByPackage[record.app.packageName] == null }
            .mapNotNull { record -> record.vendorKey }
            .filterNot { vendor -> vendor in GENERIC_VENDOR_KEYS }
            .groupingBy { vendor -> vendor }
            .eachCount()

        val groups = linkedMapOf<String, MutableList<InstalledApp>>()
        val descriptors = linkedMapOf<String, GroupDescriptor>()

        prepared.forEach { record ->
            val semantic = semanticDescriptorByPackage[record.app.packageName]
            val descriptor = when {
                semantic != null -> semantic
                record.isSystemPackage -> SYSTEM_DESCRIPTOR
                record.vendorKey != null &&
                    record.vendorKey !in GENERIC_VENDOR_KEYS &&
                    (vendorCounts[record.vendorKey] ?: 0) >= MIN_VENDOR_GROUP_SIZE -> {
                    vendorDescriptor(record.vendorKey)
                }

                else -> UTILITIES_DESCRIPTOR
            }

            descriptors[descriptor.id] = descriptor
            groups.getOrPut(descriptor.id) { mutableListOf() } += record.app
        }

        return groups.entries
            .map { (groupId, groupedApps) ->
                val descriptor = descriptors[groupId] ?: UTILITIES_DESCRIPTOR
                SmartLayoutGroup(
                    id = descriptor.id,
                    title = descriptor.title,
                    colorArgb = descriptor.colorArgb,
                    sortOrder = descriptor.sortOrder,
                    apps = groupedApps.sortedBy { app -> app.label.lowercase(Locale.ROOT) },
                )
            }
            .sortedWith(
                compareBy<SmartLayoutGroup> { group -> group.sortOrder }
                    .thenByDescending { group -> group.apps.size }
                    .thenBy { group -> group.title.lowercase(Locale.ROOT) },
            )
    }

    private fun semanticDescriptor(record: PreparedApp): GroupDescriptor? {
        val scored = SEMANTIC_DESCRIPTORS
            .map { descriptor ->
                descriptor to semanticScore(record, descriptor)
            }
            .filter { (_, score) -> score >= MIN_SEMANTIC_SCORE }
        if (scored.isEmpty()) return null
        return scored.maxBy { (_, score) -> score }.first.toGroupDescriptor()
    }

    private fun semanticScore(
        record: PreparedApp,
        descriptor: SemanticDescriptor,
    ): Int {
        val keywordScore = descriptor.keywords.sumOf { keyword ->
            if (keyword in record.tokens) KEYWORD_MATCH_SCORE else 0
        }
        val phraseScore = descriptor.phrases.sumOf { phrase ->
            if (record.searchSource.contains(phrase)) PHRASE_MATCH_SCORE else 0
        }
        val packagePrefixScore = descriptor.packagePrefixes.sumOf { prefix ->
            if (record.packageName.startsWith(prefix)) PACKAGE_PREFIX_MATCH_SCORE else 0
        }
        return keywordScore + phraseScore + packagePrefixScore
    }

    private fun vendorDescriptor(vendorKey: String): GroupDescriptor {
        val normalizedVendor = vendorKey.lowercase(Locale.ROOT)
        val title = normalizedVendor
            .split(VENDOR_SPLIT_REGEX)
            .filter { part -> part.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                }
            }
            .ifBlank { "Vendor" }
        val colorIndex = ((normalizedVendor.hashCode() % VENDOR_COLORS.size) + VENDOR_COLORS.size) %
            VENDOR_COLORS.size
        val color = VENDOR_COLORS[colorIndex]
        return GroupDescriptor(
            id = "vendor-$normalizedVendor",
            title = title,
            colorArgb = color,
            sortOrder = VENDOR_GROUP_SORT_ORDER,
        )
    }

    private data class PreparedApp(
        val app: InstalledApp,
        val packageName: String,
        val searchSource: String,
        val tokens: Set<String>,
        val vendorKey: String?,
        val isSystemPackage: Boolean,
    ) {
        companion object {
            fun from(app: InstalledApp): PreparedApp {
                val packageName = app.packageName.lowercase(Locale.ROOT)
                val label = app.label.lowercase(Locale.ROOT)
                val source = "$packageName $label"
                val tokens = source
                    .split(TOKEN_SPLIT_REGEX)
                    .asSequence()
                    .map { token -> token.trim() }
                    .filter { token -> token.length >= 2 }
                    .toSet()
                val packageSegments = packageName.split('.')
                val vendor = when {
                    packageSegments.size >= 2 -> packageSegments[1]
                    packageSegments.size == 1 -> packageSegments.first()
                    else -> null
                }?.takeIf { candidate -> candidate.isNotBlank() }
                val isSystem = SYSTEM_PACKAGE_PREFIXES.any { prefix -> packageName.startsWith(prefix) }
                return PreparedApp(
                    app = app,
                    packageName = packageName,
                    searchSource = source,
                    tokens = tokens,
                    vendorKey = vendor,
                    isSystemPackage = isSystem,
                )
            }
        }
    }

    private data class GroupDescriptor(
        val id: String,
        val title: String,
        val colorArgb: Int,
        val sortOrder: Int,
    )

    private data class SemanticDescriptor(
        val id: String,
        val title: String,
        val colorArgb: Int,
        val sortOrder: Int,
        val keywords: Set<String>,
        val phrases: Set<String> = emptySet(),
        val packagePrefixes: Set<String> = emptySet(),
    ) {
        fun toGroupDescriptor(): GroupDescriptor {
            return GroupDescriptor(
                id = id,
                title = title,
                colorArgb = colorArgb,
                sortOrder = sortOrder,
            )
        }
    }

    private val TOKEN_SPLIT_REGEX = Regex("[^\\p{L}\\p{Nd}]+")
    private val VENDOR_SPLIT_REGEX = Regex("[_\\-\\s]+")
    private const val KEYWORD_MATCH_SCORE = 3
    private const val PHRASE_MATCH_SCORE = 2
    private const val PACKAGE_PREFIX_MATCH_SCORE = 4
    private const val MIN_SEMANTIC_SCORE = 3
    private const val MIN_VENDOR_GROUP_SIZE = 2
    private const val VENDOR_GROUP_SORT_ORDER = 185

    private val SYSTEM_PACKAGE_PREFIXES = setOf(
            "com.android.",
            "android.",
            "com.google.android.",
            "com.samsung.android.",
            "com.miui.",
            "com.huawei.",
            "com.coloros.",
            "com.oplus.",
            "com.oneplus.",
        )

    private val GENERIC_VENDOR_KEYS = setOf(
            "android",
            "apps",
            "mobile",
            "launcher",
            "app",
        )

    private val VENDOR_COLORS = listOf(
            0xFF00897B.toInt(),
            0xFF1E88E5.toInt(),
            0xFF8E24AA.toInt(),
            0xFFF4511E.toInt(),
            0xFF3949AB.toInt(),
            0xFF43A047.toInt(),
            0xFF6D4C41.toInt(),
            0xFF546E7A.toInt(),
        )

    private val COMMUNICATION_DESCRIPTOR = SemanticDescriptor(
            id = "communication",
            title = "Communication",
            colorArgb = 0xFF039BE5.toInt(),
            sortOrder = 10,
            keywords = setOf(
                "telegram",
                "whatsapp",
                "signal",
                "viber",
                "line",
                "messenger",
                "chat",
                "mail",
                "gmail",
                "outlook",
                "messages",
                "dialer",
                "phone",
                "contacts",
                "sms",
                "mms",
                "teams",
                "slack",
                "discord",
                "zoom",
                "meet",
            ),
            phrases = setOf(
                "google chat",
                "microsoft teams",
            ),
        )

    private val SOCIAL_DESCRIPTOR = SemanticDescriptor(
            id = "social",
            title = "Social",
            colorArgb = 0xFF8E24AA.toInt(),
            sortOrder = 20,
            keywords = setOf(
                "instagram",
                "facebook",
                "reddit",
                "snapchat",
                "twitter",
                "threads",
                "pinterest",
                "linkedin",
                "vk",
                "ok",
                "social",
                "mastodon",
            ),
        )

    private val MEDIA_DESCRIPTOR = SemanticDescriptor(
            id = "media",
            title = "Media",
            colorArgb = 0xFFD81B60.toInt(),
            sortOrder = 30,
            keywords = setOf(
                "youtube",
                "music",
                "spotify",
                "soundcloud",
                "video",
                "tv",
                "player",
                "podcast",
                "netflix",
                "twitch",
                "stream",
                "camera",
                "gallery",
                "photo",
                "photos",
                "image",
            ),
        )

    private val GAMES_DESCRIPTOR = SemanticDescriptor(
            id = "games",
            title = "Games",
            colorArgb = 0xFFFB8C00.toInt(),
            sortOrder = 40,
            keywords = setOf(
                "game",
                "games",
                "play",
                "clash",
                "roblox",
                "minecraft",
                "steam",
                "epic",
                "riot",
                "brawl",
                "puzzle",
                "arcade",
            ),
            phrases = setOf(
                "play games",
                "google play games",
            ),
        )

    private val WORK_DESCRIPTOR = SemanticDescriptor(
            id = "work",
            title = "Work",
            colorArgb = 0xFF3949AB.toInt(),
            sortOrder = 50,
            keywords = setOf(
                "calendar",
                "docs",
                "sheets",
                "slides",
                "drive",
                "office",
                "word",
                "excel",
                "powerpoint",
                "notion",
                "todo",
                "task",
                "notes",
                "keep",
                "workspace",
                "jira",
                "confluence",
                "trello",
            ),
        )

    private val FINANCE_DESCRIPTOR = SemanticDescriptor(
            id = "finance",
            title = "Finance",
            colorArgb = 0xFF2E7D32.toInt(),
            sortOrder = 60,
            keywords = setOf(
                "bank",
                "wallet",
                "finance",
                "money",
                "invest",
                "broker",
                "coin",
                "crypto",
                "payment",
                "pay",
                "taxi",
            ),
        )

    private val SHOPPING_DESCRIPTOR = SemanticDescriptor(
            id = "shopping",
            title = "Shopping",
            colorArgb = 0xFFF4511E.toInt(),
            sortOrder = 70,
            keywords = setOf(
                "shop",
                "store",
                "market",
                "amazon",
                "aliexpress",
                "ebay",
                "ozon",
                "wildberries",
                "delivery",
                "food",
                "doordash",
                "instacart",
            ),
        )

    private val TRAVEL_DESCRIPTOR = SemanticDescriptor(
            id = "travel",
            title = "Travel & Maps",
            colorArgb = 0xFF00897B.toInt(),
            sortOrder = 80,
            keywords = setOf(
                "maps",
                "map",
                "navigator",
                "travel",
                "trip",
                "booking",
                "airline",
                "flight",
                "train",
                "metro",
                "taxi",
                "uber",
                "lyft",
                "yandexgo",
            ),
        )

    private val HEALTH_DESCRIPTOR = SemanticDescriptor(
            id = "health",
            title = "Health",
            colorArgb = 0xFF26A69A.toInt(),
            sortOrder = 90,
            keywords = setOf(
                "health",
                "fit",
                "fitness",
                "med",
                "doctor",
                "hospital",
                "pulse",
                "steps",
                "sleep",
            ),
        )

    private val EDUCATION_DESCRIPTOR = SemanticDescriptor(
            id = "education",
            title = "Education",
            colorArgb = 0xFF5E35B1.toInt(),
            sortOrder = 100,
            keywords = setOf(
                "learn",
                "study",
                "course",
                "school",
                "academy",
                "dictionary",
                "translate",
                "duolingo",
                "classroom",
            ),
        )

    private val SYSTEM_SEMANTIC_DESCRIPTOR = SemanticDescriptor(
            id = "system",
            title = "System",
            colorArgb = 0xFF607D8B.toInt(),
            sortOrder = 170,
            keywords = setOf(
                "settings",
                "updater",
                "installer",
                "launcher",
                "security",
                "permissions",
                "systemui",
            ),
            packagePrefixes = SYSTEM_PACKAGE_PREFIXES,
        )

    private val UTILITIES_SEMANTIC_DESCRIPTOR = SemanticDescriptor(
            id = "utilities",
            title = "Utilities",
            colorArgb = 0xFF546E7A.toInt(),
            sortOrder = 190,
            keywords = setOf(
                "browser",
                "chrome",
                "files",
                "file",
                "manager",
                "clock",
                "calculator",
                "weather",
                "scanner",
                "vpn",
                "tools",
            ),
        )

    private val SEMANTIC_DESCRIPTORS = listOf(
            COMMUNICATION_DESCRIPTOR,
            SOCIAL_DESCRIPTOR,
            MEDIA_DESCRIPTOR,
            GAMES_DESCRIPTOR,
            WORK_DESCRIPTOR,
            FINANCE_DESCRIPTOR,
            SHOPPING_DESCRIPTOR,
            TRAVEL_DESCRIPTOR,
            HEALTH_DESCRIPTOR,
            EDUCATION_DESCRIPTOR,
            SYSTEM_SEMANTIC_DESCRIPTOR,
            UTILITIES_SEMANTIC_DESCRIPTOR,
        )

    private val SYSTEM_DESCRIPTOR = SYSTEM_SEMANTIC_DESCRIPTOR.toGroupDescriptor()
    private val UTILITIES_DESCRIPTOR = UTILITIES_SEMANTIC_DESCRIPTOR.toGroupDescriptor()
}
