package com.youtubeauto.app.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Fetches YouTube video listings and resolves stream URLs via the Invidious public API
 * (open-source YouTube front-end — no API key required).
 * Falls back to a secondary instance if the primary is unavailable.
 */
class YouTubeRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Public Invidious instances — no auth required
    private val instances = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yt.drgnz.club"
    )

    private var currentInstance = instances[0]

    // ── Public API ──────────────────────────────────────────────────────────────

    suspend fun getTrending(): List<VideoItem> = withContext(Dispatchers.IO) {
        tryInstances { instance ->
            val json = get("$instance/api/v1/trending?type=music&fields=videoId,title,author,videoThumbnails,lengthSeconds,viewCount")
            parseVideoList(json)
        } ?: emptyList()
    }

    suspend fun search(query: String): List<VideoItem> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        tryInstances { instance ->
            val json = get("$instance/api/v1/search?q=$encoded&type=video&fields=videoId,title,author,videoThumbnails,lengthSeconds,viewCount")
            parseVideoList(json)
        } ?: emptyList()
    }

    suspend fun getStreamUrl(videoId: String): StreamInfo? = withContext(Dispatchers.IO) {
        tryInstances { instance ->
            val json = get("$instance/api/v1/videos/$videoId?fields=adaptiveFormats,formatStreams")
            parseStreamInfo(videoId, json)
        }
    }

    // ── Parsing ─────────────────────────────────────────────────────────────────

    private fun parseVideoList(json: String): List<VideoItem> {
        val arr = gson.fromJson(json, com.google.gson.JsonArray::class.java)
        return arr.mapNotNull { element ->
            runCatching {
                val obj = element.asJsonObject
                val thumbnails = obj.getAsJsonArray("videoThumbnails")
                val thumb = thumbnails?.let { pickBestThumbnail(it) } ?: ""
                VideoItem(
                    id = obj.get("videoId").asString,
                    title = obj.get("title").asString,
                    channelName = obj.get("author").asString,
                    thumbnailUrl = thumb,
                    duration = formatDuration(obj.get("lengthSeconds")?.asLong ?: 0L),
                    viewCount = formatViews(obj.get("viewCount")?.asLong ?: 0L)
                )
            }.getOrNull()
        }
    }

    private fun parseStreamInfo(videoId: String, json: String): StreamInfo? {
        val obj = gson.fromJson(json, JsonObject::class.java)

        val adaptive = obj.getAsJsonArray("adaptiveFormats")
        val formats = obj.getAsJsonArray("formatStreams")

        val stream = adaptive?.firstOrNull { el ->
            val mime = el.asJsonObject.get("type")?.asString ?: ""
            mime.startsWith("video/mp4") && el.asJsonObject.has("url")
        } ?: formats?.lastOrNull { el ->
            el.asJsonObject.has("url")
        }

        val streamObj = stream?.asJsonObject ?: return null
        return StreamInfo(
            videoId = videoId,
            streamUrl = streamObj.get("url").asString,
            mimeType = streamObj.get("type")?.asString ?: "video/mp4",
            quality = streamObj.get("qualityLabel")?.asString ?: "auto"
        )
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun get(url: String): String {
        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            return resp.body?.string() ?: throw Exception("Empty body")
        }
    }

    private fun <T> tryInstances(block: (String) -> T): T? {
        for (instance in instances) {
            try {
                val result = block(instance)
                currentInstance = instance
                return result
            } catch (e: Exception) {
                Log.w(TAG, "Instance $instance failed: ${e.message}")
            }
        }
        return null
    }

    private fun pickBestThumbnail(arr: com.google.gson.JsonArray): String {
        val preferred = listOf("medium", "default", "maxres", "sddefault")
        for (quality in preferred) {
            arr.forEach { el ->
                val obj = el.asJsonObject
                if (obj.get("quality")?.asString == quality) {
                    return obj.get("url")?.asString ?: ""
                }
            }
        }
        return arr.firstOrNull()?.asJsonObject?.get("url")?.asString ?: ""
    }

    private fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun formatViews(count: Long): String = when {
        count >= 1_000_000 -> "%.1fM views".format(count / 1_000_000.0)
        count >= 1_000 -> "%.1fK views".format(count / 1_000.0)
        else -> "$count views"
    }

    companion object {
        private const val TAG = "YouTubeRepository"
    }
}
