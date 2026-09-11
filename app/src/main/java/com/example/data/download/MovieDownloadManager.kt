package com.example.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.api.MovieBoxApiClient
import com.example.data.api.VskitShortsApiClient
import com.example.data.model.MovieItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TAG = "MovieDownloadManager"
private const val DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36"
private const val REFERER_MOVIEBOX = "https://movieboxph.org/"
private const val ORIGIN_MOVIEBOX = "https://movieboxph.org"
private const val REFERER_VSKIT = "https://vskit.online/"
private const val ORIGIN_VSKIT = "https://vskit.online"

class MovieDownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.downloadDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    init {
        // Observe downloads from database
        scope.launch {
            dao.getAllDownloads().collect { list ->
                _downloads.value = list
            }
        }
    }

    /**
     * Start or queue a download for a movie, series episode, or short video
     */
    fun startDownload(
        movie: MovieItem,
        quality: String,
        downloadUrl: String,
        dubLabel: String = "",
        seasonNumber: Int = 0,
        episodeNumber: Int = 0,
        isSeries: Boolean = false
    ) {
        val safeMovieId = movie.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "media_${System.currentTimeMillis()}" }
        val safeQuality = quality.filter { it.isLetterOrDigit() }.ifBlank { "720p" }
        val downloadId = if (isSeries && (seasonNumber > 0 || episodeNumber > 0)) {
            "${safeMovieId}_s${seasonNumber}e${episodeNumber}_${safeQuality}"
        } else {
            "${safeMovieId}_${safeQuality}"
        }

        scope.launch {
            val existing = dao.getDownloadById(downloadId)
            if (existing != null && existing.status == DownloadStatus.COMPLETED) {
                val targetFile = File(existing.localFilePath)
                if (targetFile.exists() && targetFile.length() > 0) {
                    return@launch
                }
            }

            val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "movies")
            if (!moviesDir.exists()) {
                moviesDir.mkdirs()
            }

            val safeFileName = if (isSeries && (seasonNumber > 0 || episodeNumber > 0)) {
                "${safeMovieId}_s${seasonNumber}e${episodeNumber}_${safeQuality}.mp4"
            } else {
                "${safeMovieId}_${safeQuality}.mp4"
            }
            val targetFile = File(moviesDir, safeFileName)
            val tempFile = File(moviesDir, "$safeFileName.download")

            val item = DownloadItem(
                id = downloadId,
                movieId = movie.id,
                title = movie.title,
                coverUrl = movie.coverUrl,
                backdropUrl = movie.backdropUrl,
                quality = quality,
                downloadUrl = downloadUrl.trim(),
                localFilePath = targetFile.absolutePath,
                totalBytes = 0L,
                downloadedBytes = 0L,
                progress = 0f,
                speedFormatted = "Starting...",
                status = DownloadStatus.DOWNLOADING,
                errorMessage = null,
                dubLabel = dubLabel,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                isSeries = isSeries,
                createdAt = System.currentTimeMillis()
            )

            dao.insertOrUpdate(item)
            launchDownloadJob(item, tempFile, targetFile)
        }
    }

    /**
     * Pause an ongoing download
     */
    fun pauseDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        scope.launch {
            val item = dao.getDownloadById(id) ?: return@launch
            val updated = item.copy(
                status = DownloadStatus.PAUSED,
                speedFormatted = ""
            )
            dao.update(updated)
        }
    }

    /**
     * Resume a paused or failed download
     */
    fun resumeDownload(id: String) {
        scope.launch {
            val item = dao.getDownloadById(id) ?: return@launch
            if (item.status == DownloadStatus.DOWNLOADING) return@launch

            val targetFile = File(item.localFilePath)
            val tempFile = File("${item.localFilePath}.download")
            targetFile.parentFile?.mkdirs()

            val updated = item.copy(
                status = DownloadStatus.DOWNLOADING,
                speedFormatted = "Resuming...",
                errorMessage = null
            )
            dao.update(updated)
            launchDownloadJob(updated, tempFile, targetFile)
        }
    }

    /**
     * Cancel and delete a download
     */
    fun deleteDownload(id: String) {
        activeJobs[id]?.cancel()
        activeJobs.remove(id)

        scope.launch {
            val item = dao.getDownloadById(id)
            if (item != null) {
                try {
                    val file = File(item.localFilePath)
                    if (file.exists()) file.delete()
                    val temp = File("${item.localFilePath}.download")
                    if (temp.exists()) temp.delete()
                } catch (_: Exception) {}
            }
            dao.deleteById(id)
        }
    }

    /**
     * Self-healing: resolves a fresh signed stream URL from API for movies, episodes, or shorts.
     */
    private suspend fun resolveFreshStreamUrl(item: DownloadItem): String? {
        try {
            // 1. If it's Vskit shorts
            if (item.movieId.startsWith("vskit_") || item.downloadUrl.contains("vskit") || item.downloadUrl.contains("msacdn")) {
                val cleanId = item.movieId.removePrefix("vskit_")
                val eps = VskitShortsApiClient.fetchShortsEpisodes(cleanId)
                val targetEp = eps.find { it.ep == item.episodeNumber } ?: eps.firstOrNull()
                if (targetEp != null) {
                    val targetDigits = item.quality.filter { it.isDigit() }
                    val matchingStream = targetEp.streams.find { it.resolution.contains(targetDigits) }
                        ?: targetEp.streams.firstOrNull()
                    val resolved = matchingStream?.url?.ifBlank { null } ?: targetEp.videoUrl.ifBlank { null }
                    if (!resolved.isNullOrBlank()) return resolved
                }
            }

            // 2. MovieBox movie, series episode, or short
            val isShortItem = (!item.isSeries && item.seasonNumber > 0) || item.title.contains("Short", ignoreCase = true)
            val result = MovieBoxApiClient.fetchPlayStreams(
                context = context,
                subjectId = item.movieId,
                detailPath = "",
                isShort = isShortItem,
                season = item.seasonNumber,
                episode = item.episodeNumber
            )
            val validStreams = result.streams.filter { it.url.isNotBlank() }
            if (validStreams.isNotEmpty()) {
                val targetDigits = item.quality.filter { it.isDigit() }
                val match = validStreams.firstOrNull { extractHeightDigits(it.resolution) == targetDigits }
                    ?: validStreams.firstOrNull { it.format.equals("MP4", ignoreCase = true) }
                    ?: validStreams.first()
                return match.url
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve fresh stream URL for ${item.id}: ${e.message}")
        }
        return null
    }

    private fun extractHeightDigits(res: String): String {
        return res.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
    }

    private fun launchDownloadJob(
        initialItem: DownloadItem,
        tempFile: File,
        targetFile: File
    ) {
        activeJobs[initialItem.id]?.cancel()

        val job = scope.launch {
            var currentItem = initialItem
            var currentUrl = currentItem.downloadUrl.trim()

            // 1. Resolve stream URL if blank
            if (currentUrl.isBlank()) {
                val freshUrl = resolveFreshStreamUrl(currentItem)
                if (freshUrl.isNullOrBlank()) {
                    dao.update(
                        currentItem.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Stream URL unavailable",
                            speedFormatted = ""
                        )
                    )
                    return@launch
                }
                currentUrl = freshUrl
                currentItem = currentItem.copy(downloadUrl = currentUrl)
                dao.update(currentItem)
            }

            // 2. Check if URL is HLS (.m3u8)
            if (isHlsUrl(currentUrl)) {
                try {
                    downloadHlsStream(currentItem, currentUrl, tempFile, targetFile)
                } catch (e: Exception) {
                    Log.e(TAG, "HLS download error: ${e.message}", e)
                    // If error may be due to expired URL, try refreshing once
                    val freshUrl = resolveFreshStreamUrl(currentItem)
                    if (!freshUrl.isNullOrBlank() && freshUrl != currentUrl) {
                        try {
                            currentItem = currentItem.copy(downloadUrl = freshUrl)
                            dao.update(currentItem)
                            downloadHlsStream(currentItem, freshUrl, tempFile, targetFile)
                            return@launch
                        } catch (e2: Exception) {
                            Log.e(TAG, "HLS retry error: ${e2.message}", e2)
                        }
                    }
                    if (currentItem.status == DownloadStatus.DOWNLOADING) {
                        dao.update(
                            currentItem.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = e.localizedMessage ?: "HLS download error",
                                speedFormatted = ""
                            )
                        )
                    }
                }
                return@launch
            }

            // 3. Direct download (MP4 / WebM / etc.)
            try {
                downloadDirectStream(currentItem, currentUrl, tempFile, targetFile)
            } catch (e: Exception) {
                Log.e(TAG, "Direct download error: ${e.message}", e)
                // If failed, try resolving a fresh URL once (handles expired signatures)
                val freshUrl = resolveFreshStreamUrl(currentItem)
                if (!freshUrl.isNullOrBlank() && freshUrl != currentUrl) {
                    try {
                        currentItem = currentItem.copy(downloadUrl = freshUrl)
                        dao.update(currentItem)
                        downloadDirectStream(currentItem, freshUrl, tempFile, targetFile)
                        return@launch
                    } catch (e2: Exception) {
                        Log.e(TAG, "Direct retry error: ${e2.message}", e2)
                    }
                }
                if (currentItem.status == DownloadStatus.DOWNLOADING) {
                    dao.update(
                        currentItem.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.localizedMessage ?: "Download error",
                            speedFormatted = ""
                        )
                    )
                }
            } finally {
                activeJobs.remove(initialItem.id)
            }
        }

        activeJobs[initialItem.id] = job
    }

    private fun isHlsUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("format=hls")
    }

    /**
     * Executes HTTP GET with multi-tiered headers:
     * Tier 1: movieboxph.org
     * Tier 2: vskit.online
     * Tier 3: No referer/origin
     */
    private fun executeWithHeaderFallback(
        url: String,
        existingBytes: Long = 0L
    ): Response {
        // Decide Tier 1 referer
        val isVskitOnly = url.contains("vskit.online")
        val primaryReferer = if (isVskitOnly) REFERER_VSKIT else REFERER_MOVIEBOX
        val primaryOrigin = if (isVskitOnly) ORIGIN_VSKIT else ORIGIN_MOVIEBOX

        fun buildRequest(referer: String?, origin: String?, includeRange: Boolean): Request {
            val b = Request.Builder()
                .url(url)
                .addHeader("User-Agent", DOWNLOAD_USER_AGENT)
                .addHeader("Accept", "*/*")
            if (referer != null) b.addHeader("Referer", referer)
            if (origin != null) b.addHeader("Origin", origin)
            if (includeRange && existingBytes > 0) {
                b.addHeader("Range", "bytes=$existingBytes-")
            }
            return b.build()
        }

        // 1. Try Primary
        val resp1 = okHttpClient.newCall(buildRequest(primaryReferer, primaryOrigin, true)).execute()
        if (resp1.isSuccessful) return resp1

        // If Range was rejected (416/400)
        if (existingBytes > 0 && (resp1.code == 416 || resp1.code == 400)) {
            resp1.close()
            val respRangeReset = okHttpClient.newCall(buildRequest(primaryReferer, primaryOrigin, false)).execute()
            if (respRangeReset.isSuccessful) return respRangeReset
            respRangeReset.close()
        } else {
            resp1.close()
        }

        // 2. Try Secondary (Opposite domain)
        val secondaryReferer = if (primaryReferer == REFERER_MOVIEBOX) REFERER_VSKIT else REFERER_MOVIEBOX
        val secondaryOrigin = if (primaryOrigin == ORIGIN_MOVIEBOX) ORIGIN_VSKIT else ORIGIN_MOVIEBOX
        val resp2 = okHttpClient.newCall(buildRequest(secondaryReferer, secondaryOrigin, existingBytes > 0)).execute()
        if (resp2.isSuccessful) return resp2
        resp2.close()

        // 3. Try with NO referer/origin (direct CDN pull)
        val resp3 = okHttpClient.newCall(buildRequest(null, null, existingBytes > 0)).execute()
        if (resp3.isSuccessful) return resp3

        // 4. Try with NO referer/origin and NO range
        if (existingBytes > 0) {
            resp3.close()
            return okHttpClient.newCall(buildRequest(null, null, false)).execute()
        }

        return resp3
    }

    private suspend fun downloadDirectStream(
        item: DownloadItem,
        url: String,
        tempFile: File,
        targetFile: File
    ) {
        var existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        val response = executeWithHeaderFallback(url, existingBytes)
        if (!response.isSuccessful || response.body == null) {
            val code = response.code
            response.close()
            dao.update(
                item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "Server returned HTTP $code",
                    speedFormatted = ""
                )
            )
            return
        }

        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        if (contentType.contains("mpegurl") || contentType.contains("application/x-mpegurl")) {
            val bodyString = response.body?.string() ?: ""
            response.close()
            downloadHlsStream(item, url, tempFile, targetFile, initialContent = bodyString)
            return
        }

        val isPartial = (response.code == 206)
        val resumeOffset = if (isPartial) existingBytes else 0L
        if (!isPartial && existingBytes > 0) {
            tempFile.delete()
        }

        val body = response.body!!
        val totalLength = if (isPartial) {
            resumeOffset + body.contentLength()
        } else {
            body.contentLength().takeIf { it > 0 } ?: (item.totalBytes.takeIf { it > 0 } ?: 0L)
        }

        processBodyStream(item, body, tempFile, targetFile, resumeOffset, totalLength)
    }

    private suspend fun processBodyStream(
        item: DownloadItem,
        body: okhttp3.ResponseBody,
        tempFile: File,
        targetFile: File,
        initialDownloadedBytes: Long,
        totalBytesEstimated: Long
    ) {
        val append = (initialDownloadedBytes > 0)
        var downloaded = initialDownloadedBytes
        var total = totalBytesEstimated

        var lastSpeedSampleTime = System.currentTimeMillis()
        var bytesAtLastSample = downloaded
        var lastDbUpdateTime = System.currentTimeMillis()
        var currentSpeedStr = "0 KB/s"

        body.byteStream().use { input ->
            FileOutputStream(tempFile, append).use { output ->
                val buffer = ByteArray(32768)
                var bytesRead: Int

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead

                    if (total <= 0 && body.contentLength() > 0) {
                        total = body.contentLength()
                    }

                    val now = System.currentTimeMillis()
                    // Update speed every 700ms
                    if (now - lastSpeedSampleTime >= 700) {
                        val bytesDelta = downloaded - bytesAtLastSample
                        val timeSeconds = (now - lastSpeedSampleTime) / 1000.0
                        if (timeSeconds > 0) {
                            val speedBytesPerSec = (bytesDelta / timeSeconds).toLong()
                            currentSpeedStr = formatSpeed(speedBytesPerSec)
                        }
                        lastSpeedSampleTime = now
                        bytesAtLastSample = downloaded
                    }

                    // Update DB every 900ms
                    if (now - lastDbUpdateTime >= 900) {
                        val progress = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                        dao.update(
                            item.copy(
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                progress = progress,
                                speedFormatted = currentSpeedStr,
                                status = DownloadStatus.DOWNLOADING
                            )
                        )
                        lastDbUpdateTime = now
                    }
                }
                output.flush()
            }
        }

        // Rename temp to target
        if (targetFile.exists()) targetFile.delete()
        val success = tempFile.renameTo(targetFile)
        val finalPath = if (success) targetFile.absolutePath else tempFile.absolutePath

        dao.update(
            item.copy(
                downloadedBytes = downloaded,
                totalBytes = if (total > 0) total else downloaded,
                progress = 1.0f,
                speedFormatted = "",
                localFilePath = finalPath,
                status = DownloadStatus.COMPLETED,
                errorMessage = null
            )
        )
    }

    /**
     * Complete HLS download handler:
     * - Fetches playlist
     * - Resolves master/variant playlist
     * - Extracts all segment URIs
     * - Downloads and appends media segments into tempFile
     * - Marks as COMPLETED
     */
    private suspend fun downloadHlsStream(
        item: DownloadItem,
        hlsUrl: String,
        tempFile: File,
        targetFile: File,
        initialContent: String? = null
    ) {
        val playlistText = if (!initialContent.isNullOrBlank()) {
            initialContent
        } else {
            val resp = executeWithHeaderFallback(hlsUrl, 0L)
            if (!resp.isSuccessful || resp.body == null) {
                val code = resp.code
                resp.close()
                throw IllegalStateException("Failed to load HLS playlist: HTTP $code")
            }
            val text = resp.body!!.string()
            resp.close()
            text
        }

        var mediaPlaylistUrl = hlsUrl
        var mediaPlaylistContent = playlistText

        // Check if Master Playlist
        if (playlistText.contains("#EXT-X-STREAM-INF")) {
            val targetDigits = item.quality.filter { it.isDigit() }
            val lines = playlistText.lines()
            var selectedUri: String? = null
            var highestBandwidthUri: String? = null
            var maxBandwidth = 0L

            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    val bandwidthMatch = Regex("BANDWIDTH=(\\d+)").find(line)
                    val bw = bandwidthMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    val resMatch = Regex("RESOLUTION=(\\d+x\\d+)").find(line)?.value ?: ""

                    var nextLine = ""
                    var j = i + 1
                    while (j < lines.size) {
                        val candidate = lines[j].trim()
                        if (candidate.isNotBlank() && !candidate.startsWith("#")) {
                            nextLine = candidate
                            break
                        }
                        j++
                    }

                    if (nextLine.isNotBlank()) {
                        if (bw > maxBandwidth) {
                            maxBandwidth = bw
                            highestBandwidthUri = nextLine
                        }
                        if (targetDigits.isNotBlank() && (line.contains(targetDigits) || resMatch.contains(targetDigits))) {
                            selectedUri = nextLine
                            break
                        }
                    }
                }
                i++
            }

            val chosenVariant = selectedUri ?: highestBandwidthUri
            if (!chosenVariant.isNullOrBlank()) {
                mediaPlaylistUrl = resolveUrl(hlsUrl, chosenVariant)
                val resp = executeWithHeaderFallback(mediaPlaylistUrl, 0L)
                if (resp.isSuccessful && resp.body != null) {
                    mediaPlaylistContent = resp.body!!.string()
                    resp.close()
                } else {
                    resp.close()
                }
            }
        }

        // Parse media segments
        val segmentUrls = mutableListOf<String>()
        mediaPlaylistContent.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isNotBlank() && !line.startsWith("#")) {
                segmentUrls.add(resolveUrl(mediaPlaylistUrl, line))
            }
        }

        if (segmentUrls.isEmpty()) {
            throw IllegalStateException("No media segments found in HLS playlist")
        }

        val totalSegments = segmentUrls.size
        // Start downloading segments
        var downloadedBytes = 0L
        if (tempFile.exists()) {
            downloadedBytes = tempFile.length()
        }

        var lastSpeedSampleTime = System.currentTimeMillis()
        var bytesAtLastSample = downloadedBytes
        var lastDbUpdateTime = System.currentTimeMillis()
        var currentSpeedStr = "0 KB/s"

        FileOutputStream(tempFile, downloadedBytes > 0).use { output ->
            for ((index, segUrl) in segmentUrls.withIndex()) {
                if (!scope.isActive) break

                val segResp = executeWithHeaderFallback(segUrl, 0L)
                if (segResp.isSuccessful && segResp.body != null) {
                    val segBytes = segResp.body!!.bytes()
                    output.write(segBytes)
                    downloadedBytes += segBytes.size
                    segResp.close()
                } else {
                    segResp.close()
                    // Attempt one retry for this segment
                    delay(300)
                    val retryResp = executeWithHeaderFallback(segUrl, 0L)
                    if (retryResp.isSuccessful && retryResp.body != null) {
                        val segBytes = retryResp.body!!.bytes()
                        output.write(segBytes)
                        downloadedBytes += segBytes.size
                    }
                    retryResp.close()
                }

                val now = System.currentTimeMillis()
                if (now - lastSpeedSampleTime >= 800) {
                    val bytesDelta = downloadedBytes - bytesAtLastSample
                    val timeSeconds = (now - lastSpeedSampleTime) / 1000.0
                    if (timeSeconds > 0) {
                        val speedBytesPerSec = (bytesDelta / timeSeconds).toLong()
                        currentSpeedStr = formatSpeed(speedBytesPerSec)
                    }
                    lastSpeedSampleTime = now
                    bytesAtLastSample = downloadedBytes
                }

                if (now - lastDbUpdateTime >= 1000 || index == totalSegments - 1) {
                    val progress = ((index + 1).toFloat() / totalSegments).coerceIn(0f, 1f)
                    val estimatedTotal = if (progress > 0) (downloadedBytes / progress).toLong() else 0L
                    dao.update(
                        item.copy(
                            downloadedBytes = downloadedBytes,
                            totalBytes = estimatedTotal,
                            progress = progress,
                            speedFormatted = currentSpeedStr,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )
                    lastDbUpdateTime = now
                }
            }
            output.flush()
        }

        // Rename temp to target
        if (targetFile.exists()) targetFile.delete()
        val success = tempFile.renameTo(targetFile)
        val finalPath = if (success) targetFile.absolutePath else tempFile.absolutePath

        dao.update(
            item.copy(
                downloadedBytes = downloadedBytes,
                totalBytes = downloadedBytes,
                progress = 1.0f,
                speedFormatted = "",
                localFilePath = finalPath,
                status = DownloadStatus.COMPLETED,
                errorMessage = null
            )
        )
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        val trimmed = relativeOrAbsolute.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return try {
            URI(baseUrl).resolve(trimmed).toString()
        } catch (_: Exception) {
            val lastSlash = baseUrl.lastIndexOf('/')
            if (lastSlash != -1) {
                baseUrl.substring(0, lastSlash + 1) + trimmed
            } else {
                trimmed
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MovieDownloadManager? = null

        fun getInstance(context: Context): MovieDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MovieDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
                bytesPerSec >= 1024 -> String.format("%d KB/s", bytesPerSec / 1024)
                bytesPerSec > 0 -> "$bytesPerSec B/s"
                else -> "0 KB/s"
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%d KB", bytes / 1024)
                bytes > 0 -> "$bytes B"
                else -> "0 MB"
            }
        }
    }
}
