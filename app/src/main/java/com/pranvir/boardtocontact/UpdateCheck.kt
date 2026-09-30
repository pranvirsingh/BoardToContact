package com.pranvir.boardtocontact

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub for a newer release than the installed one.
 * One plain HTTPS call, no login, no tracking. Pure logic (parsing and
 * comparing) stays separate from the network call so it can be unit-tested.
 */
object UpdateCheck {

    const val OWNER = "pranvirsingh"
    const val REPO = "BoardToContact"
    const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    const val RELEASES_PAGE = "https://github.com/$OWNER/$REPO/releases/latest"

    /** "v0.2.0" is newer than "0.1.0". Leading v optional, numeric parts. */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.trim().removePrefix("v").split(".")
        val c = current.trim().removePrefix("v").split(".")
        val size = maxOf(l.size, c.size)
        for (i in 0 until size) {
            val lv = l.getOrNull(i)?.toIntOrNull() ?: 0
            val cv = c.getOrNull(i)?.toIntOrNull() ?: 0
            if (lv != cv) return lv > cv
        }
        return false
    }

    /** Pulls "tag_name" out of a release JSON payload. Null when absent. */
    fun parseTag(json: String): String? {
        val key = "\"tag_name\""
        val at = json.indexOf(key)
        if (at < 0) return null
        val colon = json.indexOf(':', at + key.length)
        if (colon < 0) return null
        val firstQuote = json.indexOf('"', colon)
        if (firstQuote < 0) return null
        val secondQuote = json.indexOf('"', firstQuote + 1)
        if (secondQuote < 0) return null
        return json.substring(firstQuote + 1, secondQuote).ifEmpty { null }
    }

    /** Network part. Returns the latest tag, or null on any failure. */
    suspend fun fetchLatestTag(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(LATEST_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parseTag(body)
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
