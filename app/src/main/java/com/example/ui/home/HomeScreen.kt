package com.example.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.R
import com.example.data.download.DownloadStatus
import com.example.data.download.MovieDownloadManager
import com.example.data.model.AppServer
import com.example.data.model.CategorySection
import com.example.data.model.MovieItem
import com.example.data.model.toMovieItem
import com.example.ui.home.components.HeroBannerCarousel
import com.example.ui.home.components.MovieRow
import com.example.ui.home.components.NetworkStreamDialog
import com.example.ui.home.components.ShortsReelPlayer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MovieBoxRed

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNetworkStreamDialog by remember { mutableStateOf(false) }

    // Top-Level Screen Navigation Container with Smooth Animated Transitions
    AnimatedContent(
        targetState = uiState.currentScreen,
        transitionSpec = {
            if (targetState is AppScreen.Home) {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            } else {
                (slideInHorizontally(animationSpec = tween(240)) { width -> width / 4 } + fadeIn(animationSpec = tween(240)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(240)) { width -> -width / 4 } + fadeOut(animationSpec = tween(240)))
            }
        },
        label = "screen_page_navigation",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            is AppScreen.Home -> {
                HomeMainFeedView(
                    uiState = uiState,
                    onOpenSearch = { viewModel.openSearch() },
                    onOpenDownloads = { viewModel.openDownloads() },
                    onBannerClick = { viewModel.openBannerDetail(it) },
                    onPlayBanner = { banner ->
                        if (uiState.activeServer == AppServer.SERVER_2) {
                            viewModel.openShortsPlayer(banner.toMovieItem())
                        } else {
                            viewModel.openMovieDetail(banner.toMovieItem(), isFromShortsPage = false)
                        }
                    },
                    onMovieClick = { movie, playlist, isFromHotShortTv ->
                        if (uiState.activeServer == AppServer.SERVER_2 || movie.isVskitServer || isFromHotShortTv) {
                            viewModel.openShortsPlayer(movie, playlist)
                        } else {
                            viewModel.openMovieDetail(movie, playlist = playlist, isFromShortsPage = false)
                        }
                    },
                    onViewMoreClick = { section, isShorts, isLandscape ->
                        val isHotShortTv = section.isHotShortTvSection || isShorts || uiState.activeServer == AppServer.SERVER_2
                        viewModel.openSectionPage(section, isShortsSection = isHotShortTv, isLandscape = isLandscape)
                    },
                    onToggleServer = { viewModel.toggleServer() },
                    onLoadMoreRecommend = { viewModel.loadMoreVskitFilterShorts() }
                )
            }

            is AppScreen.Downloads -> {
                DownloadScreen(
                    onBackClick = { viewModel.navigateBack() },
                    onPlayOffline = { downloadItem ->
                        viewModel.playOfflineMovie(downloadItem)
                    }
                )
            }

            is AppScreen.Search -> {
                SearchScreen(
                    searchQuery = uiState.searchQuery,
                    committedSearchQuery = uiState.committedSearchQuery,
                    searchSuggestions = uiState.searchSuggestions,
                    searchResults = uiState.searchResults,
                    isSearchingSuggestions = uiState.isSearchingSuggestions,
                    isSearchingMovies = uiState.isSearchingMovies,
                    onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onSubmitSearch = { viewModel.submitSearch(it) },
                    onClearSearch = { viewModel.clearSearch() },
                    onBackClick = { viewModel.navigateBack() },
                    onMovieClick = { viewModel.openMovieDetail(it, isFromShortsPage = false) }
                )
            }

            is AppScreen.Shorts -> {
                val movie = uiState.selectedMovie ?: screen.movie
                ShortsReelPlayer(
                    movie = movie,
                    currentEpisode = "01",
                    initialIndex = 0,
                    onClose = { viewModel.navigateBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is AppScreen.Genre -> {
                val section = uiState.selectedSectionPage ?: screen.section
                val isShorts = screen.isShortsPage || uiState.isGenrePageShorts || section.isShortsSection
                val isLandscape = screen.isLandscape || uiState.isGenrePageLandscape || section.isLandscapeDetected
                GenreDetailScreen(
                    section = section,
                    movies = uiState.genrePageMovies,
                    isLoading = uiState.isGenrePageLoading,
                    isLoadingMore = uiState.isGenrePageLoadingMore,
                    hasMore = uiState.genrePageHasMore,
                    isShortsPage = isShorts,
                    isLandscape = isLandscape,
                    onBackClick = { viewModel.navigateBack() },
                    onMovieClick = { movie, playlist, isShortsFromPage ->
                        if (isShortsFromPage) {
                            viewModel.openShortsPlayer(movie, playlist)
                        } else {
                            viewModel.openMovieDetail(movie, playlist = playlist, isFromShortsPage = false)
                        }
                    },
                    onLoadMore = { viewModel.loadMoreGenreMovies() }
                )
            }

            is AppScreen.MovieDetail -> {
                val movie = uiState.selectedMovie ?: screen.movie
                MovieDetailScreen(
                    movie = movie,
                    playlist = screen.playlist,
                    isFromShortsPage = screen.isFromShortsPage,
                    isDownloaded = screen.isDownloaded,
                    downloadedSeason = screen.downloadedSeason,
                    downloadedEpisode = screen.downloadedEpisode,
                    isInWatchlist = uiState.watchlistIds.contains(movie.id),
                    onBackClick = { viewModel.navigateBack() },
                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                )
            }
        }
    }

    // Network Stream Dialog
    if (showNetworkStreamDialog) {
        NetworkStreamDialog(
            onDismiss = { showNetworkStreamDialog = false },
            onPlayStream = { streamMovie ->
                showNetworkStreamDialog = false
                viewModel.openMovieDetail(streamMovie, isFromShortsPage = false)
            }
        )
    }
}

@Composable
private fun HomeMainFeedView(
    uiState: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenDownloads: () -> Unit,
    onBannerClick: (com.example.data.model.HeroBanner) -> Unit,
    onPlayBanner: (com.example.data.model.HeroBanner) -> Unit,
    onMovieClick: (MovieItem, List<MovieItem>, Boolean) -> Unit,
    onViewMoreClick: (CategorySection, Boolean, Boolean) -> Unit,
    onToggleServer: () -> Unit,
    onLoadMoreRecommend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    val downloadManager = remember { MovieDownloadManager.getInstance(context) }
    val downloads by downloadManager.downloads.collectAsState()
    val activeDownloadsCount = remember(downloads) {
        downloads.count { it.status == DownloadStatus.DOWNLOADING }
    }

    // Infinite scroll auto-load for VSKit filter shorts
    LaunchedEffect(scrollState, uiState.activeServer, uiState.vskitFilterHasMore, uiState.isVskitFilterLoadingMore) {
        if (uiState.activeServer == AppServer.SERVER_2) {
            snapshotFlow {
                val layoutInfo = scrollState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleIndex >= totalItems - 6
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && uiState.vskitFilterHasMore && !uiState.isVskitFilterLoadingMore) {
                    onLoadMoreRecommend()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground,
                                DarkBackground.copy(alpha = 0.95f),
                                DarkBackground.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                // Header Bar (Logo, Title, Server Badge, Search Icon, Downloads Icon)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.activeServer == AppServer.SERVER_2) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1B1D24),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("https://vskit.online/logo.svg")
                                    .decoderFactory(SvgDecoder.Factory())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "VSKit Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        // MovieBox App Logo
                        Image(
                            painter = painterResource(id = R.drawable.ic_moviebox_logo),
                            contentDescription = "MovieBox Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (uiState.activeServer == AppServer.SERVER_2) "VSKit" else "MovieBox",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Dedicated Search Page Button (Square with 10dp rounded corners)
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .testTag("search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dedicated Downloads Page Button (Right of Search Icon)
                    IconButton(
                        onClick = onOpenDownloads,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .testTag("home_downloads_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Downloads",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            if (activeDownloadsCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .background(MovieBoxRed, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.activeServer == AppServer.SERVER_2) {
                // ==========================================
                // SERVER 2: SHORTS TV LAYOUT
                // ==========================================
                if (uiState.isShortsTvLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MovieBoxRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Loading VSKit...",
                                color = Color(0xFFA1A1AA),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Hero banner carousel for VSKit featured drama
                        if (uiState.shortsTvFeedData.heroBanners.isNotEmpty()) {
                            item(key = "shorts_hero_carousel") {
                                HeroBannerCarousel(
                                    banners = uiState.shortsTvFeedData.heroBanners,
                                    onBannerClick = onBannerClick,
                                    onPlayClick = onPlayBanner
                                )
                            }
                        }

                        // 2. Curated sections from VSKit (Top Searches, Trending, New Releases)
                        itemsIndexed(
                            uiState.shortsTvFeedData.sections,
                            key = { index, section -> "shorts_sec_${section.id}_$index" }
                        ) { _, section ->
                            MovieRow(
                                section = section,
                                isLandscape = false,
                                onMovieClick = { movie ->
                                    // ALL data from VSKit strictly plays in ShortsReelPlayer!
                                    onMovieClick(movie, section.items, true)
                                },
                                onViewMoreClick = { clickedSection ->
                                    onViewMoreClick(clickedSection, true, false)
                                }
                            )
                        }

                        // 3. Custom row added at the last of VSKit (from filter API channelId=1012)
                        if (uiState.vskitFilterShortsList.isNotEmpty()) {
                            val customSection = CategorySection(
                                id = "vskit_custom_row_filter_1012",
                                title = "⚡ Quick Shorts",
                                type = "VSKIT_SHORTS",
                                items = uiState.vskitFilterShortsList.take(24),
                                isVskitSection = true
                            )
                            item(key = "vskit_custom_filter_row") {
                                MovieRow(
                                    section = customSection,
                                    isLandscape = false,
                                    onMovieClick = { movie ->
                                        onMovieClick(movie, uiState.vskitFilterShortsList, true)
                                    },
                                    onViewMoreClick = { clickedSection ->
                                        onViewMoreClick(clickedSection, true, false)
                                    }
                                )
                            }

                            // 4. Infinite Auto-Loaded Feed of Quick Shorts
                            item(key = "vskit_filter_feed_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MovieBoxRed)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "🎬 All Quick Shorts",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${uiState.vskitFilterShortsList.size} loaded",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // 3-column chunk grid of all loaded Quick Shorts
                            val chunks = uiState.vskitFilterShortsList.chunked(3)
                            itemsIndexed(chunks, key = { index, _ -> "vskit_filter_chunk_$index" }) { _, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { movie ->
                                        ShortsGemCard(
                                            movie = movie,
                                            onClick = {
                                                onMovieClick(movie, uiState.vskitFilterShortsList, true)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // Infinite scroll loading indicator at bottom of list (only shown while loading and more exist)
                            if (uiState.isVskitFilterLoadingMore && uiState.vskitFilterHasMore) {
                                item(key = "auto_loading_vskit_filter") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MovieBoxRed,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        } else if (uiState.shortsTvRecommendList.isNotEmpty()) {
                            val chunks = uiState.shortsTvRecommendList.chunked(3)
                            itemsIndexed(chunks, key = { index, _ -> "gem_chunk_$index" }) { _, rowItems ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { movie ->
                                        ShortsGemCard(
                                            movie = movie,
                                            onClick = {
                                                onMovieClick(movie, uiState.shortsTvRecommendList, true)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowItems.size < 3) {
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom padding spacer so content is not covered by floating switch button
                        item(key = "shorts_bottom_spacer") {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            } else {
                // ==========================================
                // SERVER 1: MOVIEBOX LAYOUT
                // ==========================================
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MovieBoxRed,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Loading MovieBox...",
                                color = Color(0xFFA1A1AA),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Hero Banner Carousel (Clean landscape thumbnail, no shadow/gradients)
                        if (uiState.feedData.heroBanners.isNotEmpty()) {
                            item(key = "hero_carousel") {
                                HeroBannerCarousel(
                                    banners = uiState.feedData.heroBanners,
                                    onBannerClick = onBannerClick,
                                    onPlayClick = onPlayBanner
                                )
                            }
                        }

                        // 2. User's Watchlist row if items exist
                        if (uiState.watchlistIds.isNotEmpty()) {
                            val watchlistItems = uiState.feedData.sections.flatMap { it.items }
                                .filter { uiState.watchlistIds.contains(it.id) }
                                .distinctBy { it.id }

                            if (watchlistItems.isNotEmpty()) {
                                item(key = "user_watchlist") {
                                    MovieRow(
                                        section = CategorySection(
                                            id = "my_watchlist",
                                            title = "⭐ My Watchlist",
                                            type = "WATCHLIST",
                                            items = watchlistItems
                                        ),
                                        onMovieClick = { movie ->
                                            onMovieClick(movie, watchlistItems, false)
                                        },
                                        onViewMoreClick = { clickedSection ->
                                            onViewMoreClick(clickedSection, false, false)
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Curated Movie Rows from API (Dynamic detection of landscape thumbnails and 🔥Hot Short TV)
                        itemsIndexed(uiState.filteredSections, key = { index, section -> "${section.id}_${section.title}_$index" }) { _, section ->
                            val isLandscape = section.isLandscapeDetected
                            val isShortsRow = section.isShortsSection
                            MovieRow(
                                section = section,
                                isLandscape = isLandscape,
                                onMovieClick = { movie ->
                                    onMovieClick(movie, section.items, isShortsRow)
                                },
                                onViewMoreClick = { clickedSection ->
                                    onViewMoreClick(clickedSection, isShortsRow, isLandscape)
                                }
                            )
                        }

                        // Bottom padding spacer so content is not covered by floating switch button
                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(90.dp))
                        }
                    }
                }
            }

            // Floating 5px border radius server switch red button in bottom right
            ServerSwitchFloatingButton(
                activeServer = uiState.activeServer,
                onClick = onToggleServer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
            )
        }
    }
}

/**
 * Floating 5px border radius server switch red button in bottom right of homepage.
 * Toggles between Server 1 (MovieBox) and Server 2 (Shorts TV).
 */
@Composable
fun ServerSwitchFloatingButton(
    activeServer: AppServer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(5.dp), // Strictly 5px border radius as requested
        color = Color(0xFFE50914), // Red button
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
        modifier = modifier.testTag("server_switch_red_button")
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activeServer.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Vertical short drama poster card for Server 2 "Find Your Gem" grid.
 */
@Composable
fun ShortsGemCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("shorts_gem_card_${movie.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1B1D24))
        ) {
            AsyncImage(
                model = movie.coverUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Episode count badge
            if (movie.corner.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = movie.corner,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Play badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}
