package com.acuranza.app.model

/**
 * One release as published on GitHub, parsed from the Releases API response.
 *
 * @property tag the release tag, e.g. "v1.9"
 * @property name the release title, e.g. "Sumvalo v1.9"
 * @property publishedAt ISO-8601 timestamp of when the release was published
 * @property body the release notes (markdown), may be null
 * @property htmlUrl the release page URL on GitHub
 */
data class ReleaseEntry(
    val tag: String,
    val name: String,
    val publishedAt: String,
    val body: String?,
    val htmlUrl: String,
    private val providedChanges: List<ChangeItem> = emptyList(),
) {
    /** The version number without the leading "v", e.g. "1.9". */
    val version: String
        get() = tag.removePrefix("v")

    /** Short date for display, e.g. "26 Oct 2026". */
    val publishedDate: String
        get() {
            val date = publishedAt.take(10) // "YYYY-MM-DD"
            val parts = date.split("-")
            if (parts.size != 3 || parts[0].length != 4) return date
            val month = when (parts[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "May"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Aug"
                "09" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> return date
            }
            return "${parts[2]} $month ${parts[0]}"
        }

    /** Display-friendly date; falls back to "Date not recorded" when the release date is unknown. */
    val displayDate: String
        get() = publishedDate.takeIf { it.isNotEmpty() } ?: "Date not recorded"

    /** Categorised change bullets: the provided [providedChanges] list for locally-bundled
     * entries, otherwise parsed from the [body] markdown. */
    val changes: List<ChangeItem>
        get() = if (providedChanges.isNotEmpty()) providedChanges else parseFromBody()

    /** Whether this release has any changes. */
    val hasChanges: Boolean
        get() = changes.isNotEmpty()

    /** Parses change bullets from the [body] markdown. */
    private fun parseFromBody(): List<ChangeItem> {
        val bodyText = body ?: return emptyList()
        val items = mutableListOf<ChangeItem>()
        var currentCategory: ChangeCategory? = null
        for (line in bodyText.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("##") || trimmed.startsWith("#")) {
                currentCategory = ChangeCategory.fromLine(trimmed)
                continue
            }
            val firstChar = trimmed.firstOrNull()
            if (firstChar == '-' || firstChar == '*' || firstChar == '\u2022') {
                val text = trimmed.drop(1).trim()
                if (text.isNotEmpty()) {
                    items.add(ChangeItem(text, currentCategory))
                }
            } else if (currentCategory != null && trimmed.firstOrNull() == '#') {
                currentCategory = null
            }
        }
        return items
    }
}

/** One change bullet within a release. */
data class ChangeItem(
    val text: String,
    val category: ChangeCategory?,
)

/** Category of a change, parsed from markdown headers in release notes. */
enum class ChangeCategory {
    ADDED, IMPROVED, FIXED, SECURITY, OTHER;

    companion object {
        fun fromLine(line: String): ChangeCategory? {
            val lower = line.lowercase().trim()
            return when {
                lower.contains("added") || lower.contains("new") || lower.contains("feature") -> ADDED
                lower.contains("improved") || lower.contains("enhance") || lower.contains("better") -> IMPROVED
                lower.contains("fixed") || lower.contains("fix") || lower.contains("bug") || lower.contains("resolve") -> FIXED
                lower.contains("security") || lower.contains("cve") || lower.contains("vulnerab") -> SECURITY
                else -> null
            }
        }
    }
}
