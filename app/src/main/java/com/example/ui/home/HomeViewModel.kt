package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadItem
import com.example.data.model.AppServer
import com.example.data.model.CategorySection
import com.example.data.model.HeroBanner
import com.example.data.model.HomeFeedData
import com.example.data.model.MovieItem
import com.example.data.repository.MovieBoxRepository
import com.example.data.model.toMovieItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppScreen {
    data object Home : AppScreen
    data object Search : AppScreen
    data object Downloads : AppScreen
    data class Genre(
        val section: CategorySection,
        val isShortsPage: Boolean = false,
        val isLandscape: Boolean = false
    ) : AppScreen
    data class MovieDetail(
        val movie: MovieItem,
        val playlist: List<MovieItem> = emptyList(),
        val isFromShortsPage: Boolean = false,
        val isDownloaded: Boolean = false,
        val downloadedSeason: Int = 0,
        val downloadedEpisode: Int = 0
    ) : AppScreen
    data class Shorts(
        val movie: MovieItem,
        val playlist: List<MovieItem> = emptyList()
    ) : AppScreen
}

data class HomeUiState(
    val feedData: HomeFeedData = HomeFeedData(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedCategoryFilter: String = "All",
    val selectedPlatform: String? = null,
    val searchQuery: String = "",
    val committedSearchQuery: String = "",
    val searchSuggestions: List<String> = emptyList(),
    val isSearchingSuggestions: Boolean = false,
    val isSearchingMovies: Boolean = false,
    val isSearchOpen: Boolean = false,
    val selectedMovie: MovieItem? = null,
    val playingMovie: MovieItem? = null,
    val watchlistIds: Set<String> = emptySet(),
    val filteredSections: List<CategorySection> = emptyList(),
    val searchResults: List<MovieItem> = emptyList(),
    val selectedSectionPage: CategorySection? = null,
    val isGenrePageShorts: Boolean = false,
    val isGenrePageLandscape: Boolean = false,
    val genrePageMovies: List<MovieItem> = emptyList(),
    val isGenrePageLoading: Boolean = false,
    val isGenrePageLoadingMore: Boolean = false,
    val genrePageHasMore: Boolean = false,
    val genrePageCurrentPage: Int = 1,
    val screenStack: List<AppScreen> = listOf(AppScreen.Home),
    val activeServer: AppServer = AppServer.SERVER_1,
    val shortsTvFeedData: HomeFeedData = HomeFeedData(),
    val shortsTvRecommendList: List<MovieItem> = emptyList(),
    val isShortsTvLoading: Boolean = false,
    val isShortsTvRecommendLoadingMore: Boolean = false,
    val shortsTvRecommendHasMore: Boolean = true,
    val shortsTvRecommendPage: Int = 1
) {
    val currentScreen: AppScreen get() = screenStack.lastOrNull() ?: AppScreen.Home
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MovieBoxRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var suggestJob: Job? = null
    private var searchJob: Job? = null

    init {
        loadData()
        loadShortsTvData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getHomeFeedStream().collectLatest { feed ->
                _uiState.update { state ->
                    val filtered = computeFilteredSections(
                        feed.sections,
                        state.selectedCategoryFilter,
                        state.selectedPlatform
                    )
                    state.copy(
                        feedData = feed,
                        isLoading = false,
                        filteredSections = filtered
                    )
                }
            }
        }
    }

    fun switchServer(server: AppServer) {
        if (_uiState.value.activeServer == server) return
        _uiState.update { it.copy(activeServer = server) }
        if (server == AppServer.SERVER_2 && _uiState.value.shortsTvFeedData.sections.isEmpty()) {
            loadShortsTvData()
        }
    }

    fun toggleServer() {
        val nextServer = if (_uiState.value.activeServer == AppServer.SERVER_1) AppServer.SERVER_2 else AppServer.SERVER_1
        switchServer(nextServer)
    }

    fun loadShortsTvData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isShortsTvLoading = it.shortsTvFeedData.sections.isEmpty()) }
            try {
                val feed = repository.refreshShortsTvFeed()
                val (recList, hasMore) = repository.fetchShortsRecommendList(page = 1)
                _uiState.update {
                    it.copy(
                        shortsTvFeedData = feed,
                        shortsTvRecommendList = recList,
                        shortsTvRecommendHasMore = hasMore,
                        shortsTvRecommendPage = 1,
                        isShortsTvLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isShortsTvLoading = false) }
            }
        }
    }

    fun loadMoreShortsTvRecommend() {
        val current = _uiState.value
        if (current.isShortsTvRecommendLoadingMore || !current.shortsTvRecommendHasMore) return
        val nextPage = current.shortsTvRecommendPage + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isShortsTvRecommendLoadingMore = true) }
            try {
                val (moreList, hasMore) = repository.fetchShortsRecommendList(page = nextPage)
                _uiState.update {
                    it.copy(
                        shortsTvRecommendList = (it.shortsTvRecommendList + moreList).distinctBy { m -> m.id },
                        shortsTvRecommendHasMore = hasMore,
                        shortsTvRecommendPage = nextPage,
                        isShortsTvRecommendLoadingMore = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isShortsTvRecommendLoadingMore = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            if (_uiState.value.activeServer == AppServer.SERVER_2) {
                try {
                    val feed = repository.refreshShortsTvFeed()
                    val (recList, hasMore) = repository.fetchShortsRecommendList(page = 1)
                    _uiState.update {
                        it.copy(
                            shortsTvFeedData = feed,
                            shortsTvRecommendList = recList,
                            shortsTvRecommendHasMore = hasMore,
                            shortsTvRecommendPage = 1,
                            isRefreshing = false
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            } else {
                val updatedFeed = repository.refreshHomeFeed()
                _uiState.update { state ->
                    val filtered = computeFilteredSections(
                        updatedFeed.sections,
                        state.selectedCategoryFilter,
                        state.selectedPlatform
                    )
                    state.copy(
                        feedData = updatedFeed,
                        isRefreshing = false,
                        filteredSections = filtered
                    )
                }
            }
        }
    }

    fun selectCategoryFilter(filter: String) {
        _uiState.update { state ->
            val newFilter = if (state.selectedCategoryFilter == filter) "All" else filter
            val filtered = computeFilteredSections(
                state.feedData.sections,
                newFilter,
                state.selectedPlatform
            )
            state.copy(
                selectedCategoryFilter = newFilter,
                filteredSections = filtered
            )
        }
    }

    fun selectPlatform(platform: String?) {
        _uiState.update { state ->
            val newPlatform = if (state.selectedPlatform == platform) null else platform
            val filtered = computeFilteredSections(
                state.feedData.sections,
                state.selectedCategoryFilter,
                newPlatform
            )
            state.copy(
                selectedPlatform = newPlatform,
                filteredSections = filtered
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        // Only update typing input and query live search suggestions - do NOT navigate or show search results yet!
        _uiState.update {
            it.copy(
                searchQuery = query,
            )
        }

        suggestJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(
                    searchSuggestions = emptyList(),
                    isSearchingSuggestions = false,
                    committedSearchQuery = "",
                    searchResults = emptyList()
                )
            }
            return
        }

        suggestJob = viewModelScope.launch {
            delay(150) // fast debounce typing
            _uiState.update { it.copy(isSearchingSuggestions = true) }
            val suggestions = if (_uiState.value.activeServer == AppServer.SERVER_2) {
                val remoteSuggestions = try {
                    repository.fetchShortsSearchSuggestions(trimmed)
                } catch (_: Exception) {
                    emptyList()
                }
                val localItems = _uiState.value.shortsTvFeedData.sections.flatMap { it.items } + _uiState.value.shortsTvRecommendList
                val localSuggestions = localItems.map { it.title }
                    .filter { it.contains(trimmed, ignoreCase = true) }
                    .distinct()
                (remoteSuggestions + localSuggestions).distinct().take(10)
            } else {
                val remoteSuggestions = try {
                    repository.fetchSearchSuggestions(trimmed)
                } catch (_: Exception) {
                    emptyList()
                }
                val localSuggestions = if (remoteSuggestions.isEmpty()) {
                    _uiState.value.feedData.sections.flatMap { it.items }
                        .map { it.title }
                        .filter { it.contains(trimmed, ignoreCase = true) }
                        .distinct()
                        .take(8)
                } else {
                    emptyList()
                }
                (remoteSuggestions + localSuggestions).distinct().take(8)
            }
            _uiState.update {
                it.copy(
                    searchSuggestions = suggestions,
                    isSearchingSuggestions = false
                )
            }
        }
    }

    /**
     * Triggered ONLY when the user presses Enter on the keyboard OR selects a suggestion!
     * As explicitly requested: "jab tab search par type karke enter na kare ya suggetion select na kare search result search search par nhi jayega ok"
     */
    fun submitSearch(query: String? = null) {
        val q = (query ?: _uiState.value.searchQuery).trim()
        if (q.isBlank()) return

        suggestJob?.cancel()
        searchJob?.cancel()

        _uiState.update {
            it.copy(
                searchQuery = q,
                committedSearchQuery = q,
                searchSuggestions = emptyList(),
                isSearchingSuggestions = false,
                isSearchingMovies = true
            )
        }

        searchJob = viewModelScope.launch {
            if (_uiState.value.activeServer == AppServer.SERVER_2) {
                // Server 2 search
                val vskitMatches = try {
                    repository.searchShorts(q)
                } catch (_: Exception) {
                    emptyList()
                }
                val localMatches = (_uiState.value.shortsTvFeedData.sections.flatMap { it.items } + _uiState.value.shortsTvRecommendList)
                    .distinctBy { it.id }
                    .filter {
                        it.title.contains(q, ignoreCase = true) ||
                                it.genre.contains(q, ignoreCase = true) ||
                                it.description.contains(q, ignoreCase = true)
                    }

                val combined = (vskitMatches + localMatches).distinctBy { it.id }
                _uiState.update {
                    it.copy(
                        searchResults = if (combined.isNotEmpty()) combined else localMatches,
                        isSearchingMovies = false
                    )
                }
            } else {
                // Server 1 search
                val localMatches = _uiState.value.feedData.sections.flatMap { it.items }
                    .distinctBy { it.id.ifBlank { it.title } }
                    .filter {
                        it.title.contains(q, ignoreCase = true) ||
                                it.genre.contains(q, ignoreCase = true) ||
                                it.country.contains(q, ignoreCase = true)
                    }

                // Remote API search
                val apiMatches = try {
                    repository.searchMovies(q)
                } catch (_: Exception) {
                    emptyList()
                }

                val combined = (apiMatches + localMatches).distinctBy { it.id.ifBlank { it.title } }
                _uiState.update {
                    it.copy(
                        searchResults = if (combined.isNotEmpty()) combined else localMatches,
                        isSearchingMovies = false
                    )
                }
            }
        }
    }

    fun clearSearch() {
        suggestJob?.cancel()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                committedSearchQuery = "",
                searchSuggestions = emptyList(),
                searchResults = emptyList(),
                isSearchingMovies = false,
                isSearchingSuggestions = false
            )
        }
    }

    fun openSearch() {
        clearSearch()
        _uiState.update {
            it.copy(
                isSearchOpen = true,
                screenStack = it.screenStack + AppScreen.Search
            )
        }
    }

    fun toggleSearch(open: Boolean? = null) {
        val currentlyOpen = _uiState.value.currentScreen is AppScreen.Search
        val targetOpen = open ?: !currentlyOpen
        if (targetOpen) {
            openSearch()
        } else {
            navigateBack()
        }
    }

    fun openDownloads() {
        _uiState.update {
            it.copy(
                screenStack = it.screenStack + AppScreen.Downloads
            )
        }
    }

    fun playOfflineMovie(downloadItem: DownloadItem) {
        val movie = MovieItem(
            id = downloadItem.movieId,
            title = downloadItem.title,
            description = "Downloaded Offline Video (${downloadItem.quality})",
            coverUrl = downloadItem.coverUrl,
            backdropUrl = downloadItem.backdropUrl,
            rating = "10",
            genre = downloadItem.dubLabel.ifBlank { "Offline" },
            releaseDate = "",
            releaseYear = "",
            country = "",
            corner = downloadItem.quality,
            detailPath = "",
            directUrl = downloadItem.localFilePath,
            isShort = false,
            dubs = if (downloadItem.dubLabel.isNotBlank()) listOf(com.example.data.model.DubLanguage(lanName = downloadItem.dubLabel)) else emptyList()
        )
        _uiState.update {
            it.copy(
                selectedMovie = movie,
                playingMovie = movie,
                screenStack = it.screenStack + AppScreen.MovieDetail(
                    movie = movie,
                    isDownloaded = true,
                    downloadedSeason = downloadItem.seasonNumber,
                    downloadedEpisode = downloadItem.episodeNumber
                )
            )
        }
    }

    /**
     * Opens dedicated page for a row, displaying its items immediately,
     * and fetching more items via genreTopId from API.
     */
    fun openSectionPage(
        section: CategorySection,
        isShortsSection: Boolean = false,
        isLandscape: Boolean = false
    ) {
        val effectiveIsShorts = isShortsSection || section.isHotShortTvSection
        val effectiveIsLandscape = isLandscape || section.isLandscapeDetected

        _uiState.update {
            it.copy(
                selectedSectionPage = section,
                isGenrePageShorts = effectiveIsShorts,
                isGenrePageLandscape = effectiveIsLandscape,
                genrePageMovies = section.items,
                genrePageCurrentPage = 1,
                genrePageHasMore = section.genreTopId.isNotBlank(),
                isGenrePageLoading = section.genreTopId.isNotBlank(),
                screenStack = it.screenStack + AppScreen.Genre(section, effectiveIsShorts, effectiveIsLandscape)
            )
        }

        if (section.genreTopId.isNotBlank()) {
            viewModelScope.launch {
                val (fetchedMovies, hasMore) = try {
                    repository.fetchGenreRanking(section.genreTopId, page = 1, perPage = 20)
                } catch (_: Exception) {
                    Pair(emptyList(), false)
                }

                _uiState.update { state ->
                    if (state.selectedSectionPage?.id == section.id) {
                        val merged = if (fetchedMovies.isNotEmpty()) {
                            (fetchedMovies + section.items).distinctBy { it.id.ifBlank { it.title } }
                        } else {
                            section.items
                        }
                        state.copy(
                            genrePageMovies = merged,
                            genrePageHasMore = hasMore,
                            isGenrePageLoading = false
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun loadMoreGenreMovies() {
        val state = _uiState.value
        val section = state.selectedSectionPage ?: return
        if (section.genreTopId.isBlank() || !state.genrePageHasMore || state.isGenrePageLoadingMore) return

        val nextPage = state.genrePageCurrentPage + 1
        _uiState.update { it.copy(isGenrePageLoadingMore = true) }

        viewModelScope.launch {
            val (newMovies, hasMore) = try {
                repository.fetchGenreRanking(section.genreTopId, page = nextPage, perPage = 20)
            } catch (_: Exception) {
                Pair(emptyList(), false)
            }

            _uiState.update { current ->
                if (current.selectedSectionPage?.id == section.id) {
                    val updatedList = (current.genrePageMovies + newMovies).distinctBy { it.id.ifBlank { it.title } }
                    current.copy(
                        genrePageMovies = updatedList,
                        genrePageCurrentPage = nextPage,
                        genrePageHasMore = hasMore,
                        isGenrePageLoadingMore = false
                    )
                } else {
                    current.copy(isGenrePageLoadingMore = false)
                }
            }
        }
    }

    fun closeSectionPage() {
        navigateBack()
    }

    fun openMovieDetail(
        movie: MovieItem,
        playlist: List<MovieItem> = emptyList(),
        isFromShortsPage: Boolean = false
    ) {
        if (_uiState.value.activeServer == AppServer.SERVER_2 || movie.isVskitServer || movie.source.equals("vskit", ignoreCase = true)) {
            openShortsPlayer(movie, playlist)
            return
        }
        _uiState.update {
            it.copy(
                selectedMovie = movie,
                screenStack = it.screenStack + AppScreen.MovieDetail(movie, playlist, isFromShortsPage = false)
            )
        }
    }

    fun openShortsPlayer(
        movie: MovieItem,
        playlist: List<MovieItem> = emptyList()
    ) {
        _uiState.update {
            it.copy(
                selectedMovie = movie,
                screenStack = it.screenStack + AppScreen.Shorts(movie, playlist)
            )
        }
    }

    fun openBannerDetail(banner: HeroBanner) {
        val movie = banner.toMovieItem()
        if (_uiState.value.activeServer == AppServer.SERVER_2) {
            openShortsPlayer(movie)
        } else {
            openMovieDetail(movie)
        }
    }

    fun closeMovieDetail() {
        navigateBack()
    }

    fun navigateBack(): Boolean {
        val currentStack = _uiState.value.screenStack
        if (currentStack.size > 1) {
            val newStack = currentStack.dropLast(1)
            val newScreen = newStack.lastOrNull() ?: AppScreen.Home
            _uiState.update { state ->
                state.copy(
                    screenStack = newStack,
                    isSearchOpen = newScreen is AppScreen.Search,
                    selectedMovie = when (newScreen) {
                        is AppScreen.MovieDetail -> newScreen.movie
                        is AppScreen.Shorts -> newScreen.movie
                        else -> null
                    },
                    selectedSectionPage = if (newScreen is AppScreen.Genre) newScreen.section else null,
                    isGenrePageShorts = if (newScreen is AppScreen.Genre) newScreen.isShortsPage else false
                )
            }
            return true
        }
        return false
    }

    fun playMovie(movie: MovieItem) {
        openMovieDetail(movie)
    }

    fun closePlayer() {
        _uiState.update { it.copy(playingMovie = null) }
    }

    fun toggleWatchlist(movieId: String) {
        _uiState.update { state ->
            val set = state.watchlistIds.toMutableSet()
            if (set.contains(movieId)) {
                set.remove(movieId)
            } else {
                set.add(movieId)
            }
            state.copy(watchlistIds = set)
        }
    }

    private fun computeFilteredSections(
        sections: List<CategorySection>,
        categoryFilter: String,
        platform: String?
    ): List<CategorySection> {
        var result = sections

        // Platform filter if active
        if (!platform.isNullOrBlank()) {
            result = result.filter { section ->
                section.title.contains(platform, ignoreCase = true) ||
                        section.items.any { it.title.contains(platform, ignoreCase = true) }
            }
            if (result.isEmpty()) {
                // If no exact match for platform in title, return all with platform badge highlight
                result = sections
            }
        }

        // Category tabs: All, Movies, TV Series, Shorts, Anime, K-Drama
        return when (categoryFilter) {
            "Movies" -> result.filter {
                it.title.contains("Movie", ignoreCase = true) ||
                        it.title.contains("Film", ignoreCase = true)
            }
            "TV Series" -> result.filter {
                it.title.contains("Series", ignoreCase = true) ||
                        it.title.contains("Sitcom", ignoreCase = true) ||
                        it.title.contains("Show", ignoreCase = true)
            }
            "Shorts" -> result.filter {
                it.title.contains("Short", ignoreCase = true) ||
                        it.title.contains("Fight Zone", ignoreCase = true) ||
                        it.items.any { item -> item.isShort }
            }
            "Anime" -> result.filter {
                it.title.contains("Anime", ignoreCase = true) ||
                        it.title.contains("Animation", ignoreCase = true)
            }
            "K-Drama" -> result.filter {
                it.title.contains("K-Drama", ignoreCase = true) ||
                        it.title.contains("C-Drama", ignoreCase = true)
            }
            else -> result
        }
    }
}
