package com.acuranza.app.model

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONArray
import org.json.JSONObject

/** The GitHub repository that hosts Sumvalo releases. Configured once here rather than
 * scattered through the code. */
object GitHubRepo {
    val owner = "itsmeayusharyan"
    val repo = "sumvalo"
    val apiBase = "https://api.github.com/repos/$owner/$repo"
}

/** Fetches release information from the GitHub Releases API. No authentication is used, so
 * unauthenticated rate limits apply (60 requests/hour). That is acceptable for an occasional
 * changelog check.
 *
 * All network operations are blocking; callers must invoke from a coroutine dispatcher.
 */
object GitHubReleaseClient {

    private const val TIMEOUT_MS = 15_000
    private const val USER_AGENT = "Sumvalo/1.0 (+https://github.com/itsmeayusharyan/sumvalo)"

    /** Fetches all published releases, newest first. Throws [IOException] on network failure. */
    fun fetchReleases(): List<ReleaseEntry> {
        val url = URL(GitHubRepo.apiBase + "/releases?per_page=10")
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            instanceFollowRedirects = true
        }

        return try {
            val code = connection.responseCode
            if (code != 200) {
                val errorBody = connection.errorStream?.use { it.readStreamFully() }
                throw IOException("GitHub API returned $code: ${errorBody?.toString(StandardCharsets.UTF_8) ?: ""}")
            }
            val body = connection.inputStream.use { it.readStreamFully() }
            parseReleases(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseReleases(jsonBytes: ByteArray): List<ReleaseEntry> {
        val json = String(jsonBytes, StandardCharsets.UTF_8)
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val tag = obj.getString("tag_name")
            val name = obj.optString("name", tag)
            val publishedAt = obj.optString("published_at", "")
            val body = obj.optString("body", null)
            val htmlUrl = obj.getString("html_url")
            ReleaseEntry(tag, name, publishedAt, body, htmlUrl)
        }.sortedByDescending { it.publishedAt }
    }

    private fun InputStream.readStreamFully(): ByteArray {
        val buffer = java.io.ByteArrayOutputStream()
        val data = ByteArray(8192)
        var read: Int
        while (true) {
            read = this.read(data)
            if (read < 0) break
            buffer.write(data, 0, read)
        }
        return buffer.toByteArray()
    }
}
