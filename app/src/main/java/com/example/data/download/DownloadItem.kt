package com.example.data.download

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(tableName = "movie_downloads")
data class DownloadItem(
    @PrimaryKey val id: String, // e.g. "movie123_1080p"
    val movieId: String,
    val title: String,
    val coverUrl: String,
    val backdropUrl: String,
    val quality: String, // e.g. "1080p", "720p", "480p"
    val downloadUrl: String,
    val localFilePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f, // 0.0f to 1.0f
    val speedFormatted: String = "", // e.g. "2.4 MB/s"
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val dubLabel: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0,
    val isSeries: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
