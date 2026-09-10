package com.example.data.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM movie_downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM movie_downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadItem?

    @Query("SELECT * FROM movie_downloads WHERE movieId = :movieId")
    suspend fun getDownloadsForMovie(movieId: String): List<DownloadItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadItem)

    @Update
    suspend fun update(download: DownloadItem)

    @Query("DELETE FROM movie_downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
