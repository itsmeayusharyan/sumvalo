package com.acuranza.app.data.local

import android.content.Context
import com.acuranza.app.model.ReleaseEntry

/** Persists the last-fetched releases to SharedPreferences so the changelog is available offline.
 * Writes are synchronised to avoid concurrent-write exceptions.
 *
 * Releases are serialised as a simple pipe-delimited format (no JSON library needed):
 * `tag|name|publishedAt|body|htmlUrl` per line, fields base64-encoded.
 */
object ReleaseCache {

    internal const val PREFS_NAME = "changelog_store"
    private const val KEY_RELEASES = "changelog_releases"
    private const val KEY_LAST_FETCH_MS = "changelog_last_fetch_ms"

    @Volatile
    private var prefs: android.content.SharedPreferences? = null

    /** Must be called once from Application onCreate. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val sharedPrefs: android.content.SharedPreferences
        get() = prefs ?: error("ReleaseCache not initialised. Call init() first.")

    /** Cached releases, or empty list if none. */
    fun getCachedReleases(): List<ReleaseEntry> =
        sharedPrefs.getString(KEY_RELEASES, null)?.let { parseReleases(it) } ?: emptyList()

    /** Last successful fetch timestamp, or null. */
    fun getLastFetchMs(): Long? =
        sharedPrefs.getString(KEY_LAST_FETCH_MS, null)?.toLongOrNull()

    /** Saves releases and fetch timestamp atomically. */
    fun saveReleases(releases: List<ReleaseEntry>) {
        val json = releases.joinToString("\n") { r ->
            "${encode(r.tag)}|${encode(r.name)}|${encode(r.publishedAt)}|${encode(r.body ?: "")}|${encode(r.htmlUrl)}"
        }
        val editor = sharedPrefs.edit()
        editor.putString(KEY_RELEASES, json)
        editor.putString(KEY_LAST_FETCH_MS, System.currentTimeMillis().toString())
        editor.apply()
    }

    /** True if no cached data exists yet. */
    fun isEmpty(): Boolean =
        sharedPrefs.getString(KEY_RELEASES, null) == null

    private fun encode(value: String): String =
        java.util.Base64.getEncoder().encodeToString(value.toByteArray())

    private fun decode(value: String): String =
        String(java.util.Base64.getDecoder().decode(value))

    private fun parseReleases(serialized: String): List<ReleaseEntry> {
        return serialized.lines().mapNotNull { line ->
            val parts = line.split("|", limit = 5)
            if (parts.size != 5) return@mapNotNull null
            ReleaseEntry(
                tag = decode(parts[0]),
                name = decode(parts[1]),
                publishedAt = decode(parts[2]),
                body = decode(parts[3]).takeIf { it.isNotEmpty() },
                htmlUrl = decode(parts[4]),
            )
        }
    }
}
