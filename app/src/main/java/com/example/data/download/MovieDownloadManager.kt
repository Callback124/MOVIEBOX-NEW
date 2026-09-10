package com.example.data.download

import android.content.Context
import android.os.Environment
import com.example.data.model.MovieItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val DOWNLOAD_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36"
private const val DOWNLOAD_REFERER = "https://movieboxph.org/"

class MovieDownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.downloadDao()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
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
     * Start or queue a download for a movie
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
        val downloadId = if (isSeries && (seasonNumber > 0 || episodeNumber > 0)) {
            "${movie.id}_s${seasonNumber}e${episodeNumber}_${quality.filter { it.isLetterOrDigit() }}"
        } else {
            "${movie.id}_${quality.filter { it.isLetterOrDigit() }}"
        }

        scope.launch {
            val existing = dao.getDownloadById(downloadId)
            if (existing != null && existing.status == DownloadStatus.COMPLETED) {
                // Already completed
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
                "${movie.id}_s${seasonNumber}e${episodeNumber}_${quality.filter { it.isLetterOrDigit() }}.mp4"
            } else {
                "${movie.id}_${quality.filter { it.isLetterOrDigit() }}.mp4"
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
                downloadUrl = downloadUrl,
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
     * Resume a paused download
     */
    fun resumeDownload(id: String) {
        scope.launch {
            val item = dao.getDownloadById(id) ?: return@launch
            if (item.status == DownloadStatus.DOWNLOADING) return@launch

            val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "movies")
            val safeFileName = "${item.movieId}_${item.quality.filter { it.isLetterOrDigit() }}.mp4"
            val targetFile = File(moviesDir, safeFileName)
            val tempFile = File(moviesDir, "$safeFileName.download")

            val updated = item.copy(
                status = DownloadStatus.DOWNLOADING,
                speedFormatted = "Resuming..."
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

    private fun launchDownloadJob(
        item: DownloadItem,
        tempFile: File,
        targetFile: File
    ) {
        activeJobs[item.id]?.cancel()

        val job = scope.launch {
            var existingBytes = 0L
            if (tempFile.exists()) {
                existingBytes = tempFile.length()
            }

            val referer = if (item.downloadUrl.contains("vskit") || item.downloadUrl.contains("hakunaymatata") || item.downloadUrl.contains("aoneroom")) {
                "https://vskit.online/"
            } else {
                DOWNLOAD_REFERER
            }

            val requestBuilder = Request.Builder()
                .url(item.downloadUrl)
                .addHeader("User-Agent", DOWNLOAD_USER_AGENT)
                .addHeader("Referer", referer)

            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            try {
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    // Try without range header if range was rejected (e.g. 416)
                    if (existingBytes > 0 && (response.code == 416 || response.code == 400)) {
                        tempFile.delete()
                        existingBytes = 0L
                        val fallbackRequest = Request.Builder()
                            .url(item.downloadUrl)
                            .addHeader("User-Agent", DOWNLOAD_USER_AGENT)
                            .addHeader("Referer", DOWNLOAD_REFERER)
                            .build()
                        val fallbackResp = okHttpClient.newCall(fallbackRequest).execute()
                        val fallbackBody = fallbackResp.body
                        if (!fallbackResp.isSuccessful || fallbackBody == null) {
                            dao.update(item.copy(status = DownloadStatus.FAILED, errorMessage = "HTTP ${fallbackResp.code}"))
                            return@launch
                        }
                        processBodyStream(item, fallbackBody, tempFile, targetFile, 0L, fallbackBody.contentLength())
                    } else {
                        dao.update(item.copy(status = DownloadStatus.FAILED, errorMessage = "HTTP ${response.code}"))
                        return@launch
                    }
                } else {
                    val isPartial = (response.code == 206)
                    val resumeOffset = if (isPartial) existingBytes else 0L
                    if (!isPartial && existingBytes > 0) {
                        tempFile.delete()
                    }
                    val totalContentLength = if (isPartial) {
                        resumeOffset + body.contentLength()
                    } else {
                        body.contentLength().takeIf { it > 0 } ?: (item.totalBytes.takeIf { it > 0 } ?: 0L)
                    }

                    processBodyStream(item, body, tempFile, targetFile, resumeOffset, totalContentLength)
                }
            } catch (e: Exception) {
                if (item.status == DownloadStatus.DOWNLOADING) {
                    dao.update(
                        item.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.localizedMessage ?: "Download error",
                            speedFormatted = ""
                        )
                    )
                }
            } finally {
                activeJobs.remove(item.id)
            }
        }

        activeJobs[item.id] = job
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
                val buffer = ByteArray(8192)
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

                    // Update DB every 1000ms
                    if (now - lastDbUpdateTime >= 1000) {
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
                status = DownloadStatus.COMPLETED
            )
        )
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
