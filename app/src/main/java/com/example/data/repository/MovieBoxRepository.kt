package com.example.data.repository

import android.content.Context
import com.example.data.api.MovieBoxApiClient
import com.example.data.api.VskitShortsApiClient
import com.example.data.model.HomeFeedData
import com.example.data.model.MovieItem
import com.example.data.model.VskitEpisodeItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MovieBoxRepository(private val context: Context) {

    /**
     * Emits the instant local cache first for 0% lag startup,
     * then queries the live MovieBox API and emits updated feed.
     */
    fun getHomeFeedStream(): Flow<HomeFeedData> = flow {
        // 1. Instant emit from cached bundle (< 5ms)
        val initialData = MovieBoxApiClient.loadBundledHomeFeed(context)
        emit(initialData)

        // 2. Fetch live data from network
        try {
            val liveData = MovieBoxApiClient.fetchHomeFeed(context)
            if (liveData.sections.isNotEmpty() || liveData.heroBanners.isNotEmpty()) {
                emit(liveData)
            }
        } catch (_: Exception) {
            // If offline, initialData already emitted
        }
    }

    suspend fun refreshHomeFeed(): HomeFeedData {
        return MovieBoxApiClient.fetchHomeFeed(context)
    }

    /**
     * Server 2: Shorts TV feed stream
     */
    fun getShortsTvFeedStream(): Flow<HomeFeedData> = flow {
        try {
            val liveData = VskitShortsApiClient.fetchShortsTabOperations()
            if (liveData.sections.isNotEmpty() || liveData.heroBanners.isNotEmpty()) {
                emit(liveData)
            }
        } catch (_: Exception) {
        }
    }

    suspend fun refreshShortsTvFeed(): HomeFeedData {
        return VskitShortsApiClient.fetchShortsTabOperations()
    }

    suspend fun fetchShortsRecommendList(page: Int = 1, perPage: Int = 20): Pair<List<MovieItem>, Boolean> {
        return VskitShortsApiClient.fetchShortsRecommendList(page, perPage)
    }

    suspend fun fetchFilterShortsList(
        page: Int = 1,
        perPage: Int = 24,
        channelId: Int = 1012
    ): Pair<List<MovieItem>, Boolean> {
        return VskitShortsApiClient.fetchFilterShortsList(page, perPage, channelId)
    }

    suspend fun fetchShortsEpisodes(subjectId: String): List<VskitEpisodeItem> {
        return VskitShortsApiClient.fetchShortsEpisodes(subjectId)
    }

    suspend fun searchShorts(keyword: String, page: Int = 1, perPage: Int = 20): List<MovieItem> {
        return VskitShortsApiClient.searchShorts(keyword, page, perPage)
    }

    suspend fun fetchEveryoneSearchShorts(): List<MovieItem> {
        return VskitShortsApiClient.fetchEveryoneSearch()
    }

    suspend fun fetchShortsSearchSuggestions(keyword: String): List<String> {
        return VskitShortsApiClient.fetchSearchSuggestions(keyword)
    }

    suspend fun fetchSearchSuggestions(keyword: String): List<String> {
        return MovieBoxApiClient.fetchSearchSuggestions(context, keyword)
    }

    suspend fun fetchGenreRanking(
        genreTopId: String,
        page: Int = 1,
        perPage: Int = 20
    ): Pair<List<MovieItem>, Boolean> {
        return MovieBoxApiClient.fetchRankingListContent(context, genreTopId, page, perPage)
    }

    suspend fun searchMovies(keyword: String): List<MovieItem> {
        return MovieBoxApiClient.searchMoviesApi(context, keyword)
    }

    suspend fun fetchPlayStreams(
        subjectId: String,
        detailPath: String = "",
        isShort: Boolean = false,
        season: Int? = null,
        episode: Int? = null
    ): com.example.data.model.StreamPlayResult {
        return MovieBoxApiClient.fetchPlayStreams(context, subjectId, detailPath, isShort, season, episode)
    }
}


