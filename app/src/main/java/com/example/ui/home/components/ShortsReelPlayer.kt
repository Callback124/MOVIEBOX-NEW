package com.example.ui.home.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.api.VskitShortsApiClient
import com.example.data.model.VskitEpisodeItem
import com.example.data.download.MovieDownloadManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.api.MovieBoxApiClient
import com.example.data.model.MovieItem
import com.example.data.model.MovieStream
import com.example.data.model.SubjectDetailResult
import com.example.ui.theme.MovieBoxRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FALLBACK_SHORT_STREAM =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun hideSystemUI(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )
    }
}

private fun showSystemUI(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}

private fun formatShortTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = (millis / 1000).toInt()
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(UnstableApi::class)
private fun buildShortMediaSource(streamUrl: String, format: String): MediaSource {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36")
        .setDefaultRequestProperties(
            mapOf(
                "Referer" to "https://movieboxph.org/",
                "Origin" to "https://movieboxph.org"
            )
        )
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)

    val mediaItem = MediaItem.fromUri(streamUrl)
    val upper = format.uppercase()
    return when {
        upper.contains("M3U8") || upper.contains("HLS") || streamUrl.contains(".m3u8") -> {
            HlsMediaSource.Factory(httpDataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        }
        upper.contains("MPD") || upper.contains("DASH") || streamUrl.contains(".mpd") -> {
            DashMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(mediaItem)
        }
        else -> {
            ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }
}

data class ReelEpisodeItem(
    val id: String,
    val movie: MovieItem,
    val season: Int,
    val episode: Int,
    val episodeFormatted: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val directStreamUrl: String = ""
)

/**
 * Dedicated Fullscreen Shorts Reel Player:
 * - Fullscreen stays strictly in portrait (no landscape)
 * - Vertical reel layout with manual swipe up/down to change episodes/shorts
 * - Auto-plays next episode with smooth reel scroll animation when current episode ends
 * - Setting button (gear icon) visible on top bar and side action bar
 * - Opens Quality & Audio Settings modal dialog (scrollable, accessible OK/Cancel)
 * - Sleek normal seekbar for seeking
 * - Top back/close button to return to detail view
 */
@OptIn(UnstableApi::class)
@Composable
fun ShortsReelPlayer(
    movie: MovieItem,
    episodes: List<String> = emptyList(),
    currentEpisode: String = "01",
    selectedSeason: Int = 1,
    playlist: List<MovieItem> = emptyList(),
    initialIndex: Int = -1,
    onClose: () -> Unit,
    onEpisodeChanged: (String) -> Unit = {},
    onMovieChanged: (MovieItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val scope = rememberCoroutineScope()

    // Fetch Subject Detail or Vskit Episodes for movie
    var detailedInfo by remember(movie.id, movie.detailPath) {
        mutableStateOf<SubjectDetailResult?>(null)
    }
    var vskitEpisodes by remember(movie.id) {
        mutableStateOf<List<VskitEpisodeItem>>(emptyList())
    }

    LaunchedEffect(movie.id, movie.detailPath, movie.isVskitServer) {
        val subjectId = movie.id.ifBlank { movie.detailPath }
        if (movie.isVskitServer || movie.source.equals("vskit", ignoreCase = true) || subjectId.length > 15) {
            val eps = VskitShortsApiClient.fetchShortsEpisodes(subjectId)
            if (eps.isNotEmpty()) {
                vskitEpisodes = eps
            }
        } else if (movie.id.isNotBlank() || movie.detailPath.isNotBlank()) {
            val res = MovieBoxApiClient.fetchSubjectDetail(
                context = context,
                detailPath = movie.detailPath,
                fallbackSubjectId = movie.id
            )
            if (res != null) {
                detailedInfo = res
            }
        }
    }

    // Resolve episodes of this short drama:
    val resolvedEpisodes = remember(episodes, detailedInfo, vskitEpisodes, movie.corner, movie.totalEpisodes) {
        if (vskitEpisodes.isNotEmpty()) {
            vskitEpisodes.map { String.format("%02d", it.ep) }
        } else if (episodes.size > 1) {
            episodes
        } else {
            val fromDetail = detailedInfo?.seasons?.find { it.seasonNumber == selectedSeason }?.maxEp
                ?: detailedInfo?.seasons?.firstOrNull()?.maxEp
            if (fromDetail != null && fromDetail > 1) {
                (1..fromDetail).map { String.format("%02d", it) }
            } else if (movie.totalEpisodes > 1) {
                (1..movie.totalEpisodes).map { String.format("%02d", it) }
            } else {
                val cornerEp = movie.corner.filter { it.isDigit() }.toIntOrNull()
                val epCount = if (cornerEp != null && cornerEp in 2..500) cornerEp else 60
                (1..epCount).map { String.format("%02d", it) }
            }
        }
    }

    // Build the reel episode items:
    // Each reel page is an EPISODE of this short drama!
    val reelItems = remember(movie, resolvedEpisodes, vskitEpisodes, selectedSeason) {
        resolvedEpisodes.mapIndexed { index, epStr ->
            val epNum = epStr.toIntOrNull() ?: (index + 1)
            val vskitItem = vskitEpisodes.find { it.ep == epNum }
            ReelEpisodeItem(
                id = "${movie.id}_s${selectedSeason}_ep_${epNum}",
                movie = movie,
                season = if (selectedSeason > 0) selectedSeason else 1,
                episode = epNum,
                episodeFormatted = epStr,
                title = movie.title,
                subtitle = "Episode $epNum",
                description = movie.description,
                directStreamUrl = vskitItem?.videoUrl ?: ""
            )
        }
    }

    val safeList = remember(reelItems) {
        if (reelItems.isEmpty()) {
            listOf(
                ReelEpisodeItem(
                    id = "empty",
                    movie = movie,
                    season = 1,
                    episode = 1,
                    episodeFormatted = "01",
                    title = movie.title.ifBlank { "Short Video" },
                    subtitle = "Episode 1",
                    description = ""
                )
            )
        } else reelItems
    }

    val computedInitialIndex = remember(safeList, currentEpisode, initialIndex) {
        if (initialIndex in safeList.indices && initialIndex > 0) {
            initialIndex
        } else {
            val targetEp = currentEpisode.toIntOrNull() ?: 1
            safeList.indexOfFirst { it.episode == targetEp }.coerceIn(0, (safeList.size - 1).coerceAtLeast(0))
        }
    }

    val pagerState = rememberPagerState(
        initialPage = computedInitialIndex,
        pageCount = { safeList.size }
    )

    // Enforce Portrait Orientation and Hide System UI while in Shorts Fullscreen
    DisposableEffect(Unit) {
        activity?.runOnUiThread {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            hideSystemUI(activity)
        }
        onDispose {
            activity?.runOnUiThread {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                showSystemUI(activity)
            }
        }
    }

    // Single ExoPlayer instance for optimal resource usage
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isFetchingStream by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showChooseEpisodeModal by remember { mutableStateOf(false) }
    val streamCache = remember { mutableMapOf<String, Pair<String, String>>() }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }

    // Intercept back button
    BackHandler {
        if (showChooseEpisodeModal) {
            showChooseEpisodeModal = false
        } else if (showDownloadDialog) {
            showDownloadDialog = false
        } else if (isLocked) {
            isLocked = false
        } else {
            onClose()
        }
    }

    // Settings Modal State (Quality & Audio selection)
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("Auto") }
    var availableVideoQualities by remember { mutableStateOf(listOf("Auto", "1080p", "720p", "480p")) }
    var currentEpisodeStreams by remember { mutableStateOf<List<MovieStream>>(emptyList()) }
    var availableAudioTracks by remember { mutableStateOf<List<PlayerAudioTrack>>(emptyList()) }
    var currentAudioTrackId by remember { mutableStateOf("auto") }

    // Auto-hide controls state for Shorts Player
    var areControlsVisible by remember { mutableStateOf(true) }

    // Toggle Play/Pause helper
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPlaying = false
            showPlayPauseIndicator = true
            areControlsVisible = true
        } else {
            exoPlayer.play()
            isPlaying = true
            showPlayPauseIndicator = true
        }
    }

    // Auto-dismiss play/pause flash indicator
    LaunchedEffect(showPlayPauseIndicator) {
        if (showPlayPauseIndicator) {
            delay(800)
            showPlayPauseIndicator = false
        }
    }

    // Auto-hide controls after 3.5 seconds during playback
    LaunchedEffect(areControlsVisible, isPlaying, isSeeking, showSettingsDialog) {
        if (areControlsVisible && isPlaying && !isSeeking && !showSettingsDialog) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Always reveal controls briefly on episode swipe
    LaunchedEffect(pagerState.currentPage) {
        areControlsVisible = true
    }

    // Function to extract audio tracks from exoPlayer
    fun updateAudioTracks() {
        val tracks = mutableListOf<PlayerAudioTrack>()
        tracks.add(PlayerAudioTrack("auto", "Default Audio"))
        val currentTracks = exoPlayer.currentTracks
        for (i in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[i]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (j in 0 until group.length) {
                    val format = group.getTrackFormat(j)
                    val lang = format.language?.uppercase() ?: "Track ${j + 1}"
                    val label = format.label ?: lang
                    tracks.add(
                        PlayerAudioTrack(
                            id = "${i}_${j}",
                            label = label,
                            groupIndex = i,
                            trackIndex = j
                        )
                    )
                }
            }
        }
        availableAudioTracks = tracks
    }

    // Notify active short / episode change to caller
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page in safeList.indices) {
                val item = safeList[page]
                onEpisodeChanged(item.episodeFormatted)
                onMovieChanged(item.movie)
            }
        }
    }

    // ExoPlayer Listener: handles buffering, readiness, error, audio tracks, and AUTO-PLAY NEXT WITH SCROLL ANIMATION
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        durationMs = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                        updateAudioTracks()
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        // AUTO PLAY NEXT EPISODE WITH SMOOTH SCROLL ANIMATION (Reel Style!)
                        scope.launch {
                            val nextIndex = pagerState.currentPage + 1
                            if (nextIndex < safeList.size) {
                                pagerState.animateScrollToPage(nextIndex)
                            } else {
                                // Replay from start if at the end of playlist/episodes
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateAudioTracks()
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                scope.launch {
                    try {
                        val fallback = buildShortMediaSource(FALLBACK_SHORT_STREAM, "MP4")
                        exoPlayer.setMediaSource(fallback)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (_: Exception) {}
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress updater loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isSeeking) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }
            delay(250)
        }
    }

    // Background pre-fetching for next and previous episode
    LaunchedEffect(pagerState.currentPage, safeList) {
        val nextIndex = pagerState.currentPage + 1
        val prevIndex = pagerState.currentPage - 1
        val itemsToPrefetch = listOfNotNull(safeList.getOrNull(nextIndex), safeList.getOrNull(prevIndex))
        itemsToPrefetch.forEach { item ->
            val cacheKey = "${item.movie.id}_${item.season}_${item.episode}"
            if (!streamCache.containsKey(cacheKey)) {
                launch(Dispatchers.IO) {
                    try {
                        if (item.directStreamUrl.isNotBlank()) {
                            streamCache[cacheKey] = Pair(item.directStreamUrl, "MP4")
                        } else if (item.movie.isVskitServer || item.movie.source.equals("vskit", ignoreCase = true)) {
                            val eps = VskitShortsApiClient.fetchShortsEpisodes(item.movie.id)
                            val match = eps.find { it.ep == item.episode }
                            if (match != null && match.videoUrl.isNotBlank()) {
                                streamCache[cacheKey] = Pair(match.videoUrl, "MP4")
                            }
                        } else if (item.movie.directUrl.isNotBlank()) {
                            streamCache[cacheKey] = Pair(item.movie.directUrl, "MP4")
                        } else {
                            val res = MovieBoxApiClient.fetchPlayStreams(
                                context = context,
                                subjectId = item.movie.id,
                                detailPath = item.movie.detailPath,
                                isShort = true,
                                season = item.season,
                                episode = item.episode
                            )
                            val chosen = res.defaultStream ?: res.streams.firstOrNull()
                            if (chosen != null && chosen.url.isNotBlank()) {
                                streamCache[cacheKey] = Pair(chosen.url, chosen.format)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Fetch and load stream whenever pager page changes
    LaunchedEffect(pagerState.currentPage) {
        val currentItem = safeList.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        isBuffering = true
        isFetchingStream = true

        // Direct next/previous episode load: stop & clear previous media immediately
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val cacheKey = "${currentItem.movie.id}_${currentItem.season}_${currentItem.episode}"
        val cached = streamCache[cacheKey]

        val streamUrl: String
        val format: String

        if (cached != null && cached.first.isNotBlank()) {
            streamUrl = cached.first
            format = cached.second
            isFetchingStream = false
        } else if (currentItem.directStreamUrl.isNotBlank()) {
            streamUrl = currentItem.directStreamUrl
            format = "MP4"
            currentEpisodeStreams = emptyList()
            availableVideoQualities = listOf("Auto", "720p", "480p")
            streamCache[cacheKey] = Pair(streamUrl, format)
            isFetchingStream = false
        } else if (currentItem.movie.isVskitServer || currentItem.movie.source.equals("vskit", ignoreCase = true)) {
            val eps = VskitShortsApiClient.fetchShortsEpisodes(currentItem.movie.id)
            val match = eps.find { it.ep == currentItem.episode } ?: eps.firstOrNull()
            streamUrl = match?.videoUrl ?: FALLBACK_SHORT_STREAM
            format = "MP4"
            currentEpisodeStreams = emptyList()
            availableVideoQualities = listOf("Auto", "720p", "480p")
            if (streamUrl != FALLBACK_SHORT_STREAM) {
                streamCache[cacheKey] = Pair(streamUrl, format)
            }
            isFetchingStream = false
        } else if (currentItem.movie.directUrl.isNotBlank()) {
            streamUrl = currentItem.movie.directUrl
            format = "MP4"
            currentEpisodeStreams = emptyList()
            availableVideoQualities = listOf("Auto", "1080p", "720p", "480p")
            streamCache[cacheKey] = Pair(streamUrl, format)
            isFetchingStream = false
        } else {
            val streamResult = MovieBoxApiClient.fetchPlayStreams(
                context = context,
                subjectId = currentItem.movie.id,
                detailPath = currentItem.movie.detailPath,
                isShort = true,
                season = currentItem.season,
                episode = currentItem.episode
            )
            currentEpisodeStreams = streamResult.streams

            val streamQualities = streamResult.streams.map { s ->
                val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "720" } ?: "720"
                when (clean) {
                    "1080" -> "1920 × 1080"
                    "720" -> "1280 × 720"
                    "480" -> "854 × 480"
                    "360" -> "640 × 360"
                    else -> "${clean}p"
                }
            }.distinct()

            availableVideoQualities = if (streamQualities.isNotEmpty()) {
                listOf("Auto") + streamQualities.filter { !it.equals("Auto", ignoreCase = true) }
            } else {
                listOf("Auto", "1920 × 1080", "1280 × 720", "854 × 480")
            }

            val digits = selectedQuality.filter { it.isDigit() }
            val chosenStream = if (digits.isNotBlank() && !selectedQuality.equals("Auto", ignoreCase = true)) {
                streamResult.streams.firstOrNull { s ->
                    val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                    clean.contains(digits) || digits.contains(clean)
                } ?: streamResult.defaultStream ?: streamResult.streams.firstOrNull()
            } else {
                streamResult.defaultStream ?: streamResult.streams.firstOrNull()
            }

            streamUrl = chosenStream?.url?.ifBlank { null } ?: FALLBACK_SHORT_STREAM
            format = chosenStream?.format ?: "MP4"
            if (streamUrl != FALLBACK_SHORT_STREAM) {
                streamCache[cacheKey] = Pair(streamUrl, format)
            }
            isFetchingStream = false
        }

        try {
            val mediaSource = buildShortMediaSource(streamUrl, format)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (_: Exception) {
            val fallback = buildShortMediaSource(FALLBACK_SHORT_STREAM, "MP4")
            exoPlayer.setMediaSource(fallback)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Vertical Pager for Shorts & Episodes (Manual swipe up / down changes episode/short with reel physics)
        VerticalPager(
            state = pagerState,
            userScrollEnabled = !isLocked,
            modifier = Modifier.fillMaxSize(),
            key = { index -> safeList.getOrNull(index)?.id ?: index }
        ) { page ->
            val shortItem = safeList[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isLocked) {
                            togglePlayPause()
                        } else {
                            areControlsVisible = !areControlsVisible
                        }
                    }
            ) {
                // Video Surface (Only attached to ExoPlayer if current active page)
                if (page == pagerState.currentPage) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                this.resizeMode = resizeMode
                                keepScreenOn = true
                            }
                        },
                        update = { pv ->
                            pv.player = exoPlayer
                            pv.resizeMode = resizeMode
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Centered Buffering Spinner (Requirement: Center loader in player)
                if (page == pagerState.currentPage && (isBuffering || isFetchingStream)) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MovieBoxRed,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // Play / Pause Indicator Flash or Paused State (Tapping in center or paused)
                AnimatedVisibility(
                    visible = (showPlayPauseIndicator || !isPlaying) && page == pagerState.currentPage,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(72.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                togglePlayPause()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                // Top Controls Bar (Auto-hides with controls visibility, hidden when locked)
                AnimatedVisibility(
                    visible = areControlsVisible && !isLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Episode / Shorts Badge (Back icon removed per user request: "shortsplayer me back icon hata do")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MovieBoxRed
                        ) {
                            Text(
                                text = if (safeList.size > 1 && episodes.size > 1) "EP ${shortItem.episodeFormatted}" else "SHORTS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Counter Indicator (e.g. 5 / 24)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.45f)
                        ) {
                            Text(
                                text = "${page + 1} / ${safeList.size}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // SETTINGS BUTTON (Gear Icon in Top Bar - Quality & Audio modal)
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Playback Settings",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Right Side Vertical Action Bar (Auto-hides with controls visibility, hidden when locked)
                AnimatedVisibility(
                    visible = areControlsVisible && !isLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Lock Screen Button
                        IconButton(
                            onClick = {
                                isLocked = true
                                areControlsVisible = false
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Screen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Download Episode Button
                        IconButton(
                            onClick = {
                                showDownloadDialog = true
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Episode",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Aspect Ratio Toggle (Zoom / Fit)
                        IconButton(
                            onClick = {
                                resizeMode = if (resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                } else {
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Toggle Fit/Fill",
                                tint = Color.White
                            )
                        }

                        // Mute / Unmute Toggle
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }

                        // Choose Episode Button (Bottom option in action bar - opens grid sheet)
                        IconButton(
                            onClick = {
                                showChooseEpisodeModal = true
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Widgets,
                                contentDescription = "Choose Episode",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Floating Unlock Button when Screen is Locked
                AnimatedVisibility(
                    visible = isLocked && areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = {
                            isLocked = false
                            areControlsVisible = true
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock Screen",
                            tint = MovieBoxRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Dark Gradient Scrim & Content Details (Title, EP, Detail, Seekbar)
                // Auto-shows and auto-hides with playback controls, exactly as shown in the screenshot
                AnimatedVisibility(
                    visible = areControlsVisible && !isLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.92f)
                                    )
                                )
                            )
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Title
                            Text(
                                text = shortItem.title,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Subtitle: Episode X • Description (Matches screenshot layout)
                            val epNumber = shortItem.episodeFormatted.toIntOrNull() ?: shortItem.episodeFormatted
                            val detailSynopsis = (detailedInfo?.description?.ifBlank { null }
                                ?: shortItem.description.ifBlank { null }
                                ?: shortItem.movie.description.ifBlank { null }
                                ?: shortItem.subtitle).trim()

                            val subtitleText = if (detailSynopsis.isNotBlank()) {
                                "Episode $epNumber • $detailSynopsis"
                            } else {
                                "Episode $epNumber"
                            }

                            Text(
                                text = subtitleText,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Timestamps Row: Left current time, Right total duration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatShortTime(currentPositionMs),
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = formatShortTime(durationMs),
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            // Normal Sleek Seekbar
                            NormalPlayerSeekBar(
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                onSeekTo = { targetMs ->
                                    exoPlayer.seekTo(targetMs)
                                    currentPositionMs = targetMs
                                },
                                onSeekingChange = { seeking, targetMs ->
                                    isSeeking = seeking
                                    if (seeking) {
                                        currentPositionMs = targetMs
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // SETTINGS MODAL DIALOG (Quality & Audio tracks selection - Scrollable & fits fullscreen!)
        if (showSettingsDialog) {
            VideoAudioSettingsDialog(
                show = showSettingsDialog,
                onDismissRequest = { showSettingsDialog = false },
                videoResolutionOptions = availableVideoQualities,
                currentVideoQuality = selectedQuality,
                onVideoQualitySelected = { newQuality ->
                    selectedQuality = newQuality
                    val digits = newQuality.filter { it.isDigit() }
                    val matchedStream = if (digits.isNotBlank() && !newQuality.equals("Auto", ignoreCase = true)) {
                        currentEpisodeStreams.firstOrNull { s ->
                            val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                            clean.contains(digits) || digits.contains(clean)
                        }
                    } else null
                    val targetStream = matchedStream
                        ?: currentEpisodeStreams.firstOrNull()
                    if (targetStream != null && targetStream.url.isNotBlank()) {
                        val currentPos = exoPlayer.currentPosition
                        val mediaSource = buildShortMediaSource(targetStream.url, targetStream.format)
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()
                        if (currentPos > 0) exoPlayer.seekTo(currentPos)
                        exoPlayer.play()
                    }
                },
                availableAudioTracks = availableAudioTracks,
                currentAudioTrackId = currentAudioTrackId,
                onAudioTrackSelected = { selectedAudio ->
                    currentAudioTrackId = selectedAudio.id
                    if (selectedAudio.groupIndex >= 0) {
                        try {
                            val trackGroup = exoPlayer.currentTracks.groups[selectedAudio.groupIndex]
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setOverrideForType(
                                    TrackSelectionOverride(
                                        trackGroup.mediaTrackGroup,
                                        listOf(selectedAudio.trackIndex)
                                    )
                                )
                                .build()
                        } catch (_: Exception) {}
                    }
                }
            )
        }

        // DOWNLOAD QUALITY CHOOSE MODAL DIALOG
        if (showDownloadDialog) {
            val currentItem = safeList.getOrNull(pagerState.currentPage)
            if (currentItem != null) {
                val vskitItem = vskitEpisodes.find { it.ep == currentItem.episode }
                val resolvedStreamUrl = currentItem.directStreamUrl.ifBlank {
                    vskitItem?.videoUrl ?: ""
                }
                val streamsForDownload = if (currentEpisodeStreams.isNotEmpty()) {
                    currentEpisodeStreams
                } else if (resolvedStreamUrl.isNotBlank()) {
                    listOf(
                        MovieStream(
                            id = "shorts_${currentItem.episode}_720",
                            url = resolvedStreamUrl,
                            resolution = "720p",
                            format = "mp4",
                            size = 15000000L
                        ),
                        MovieStream(
                            id = "shorts_${currentItem.episode}_480",
                            url = resolvedStreamUrl,
                            resolution = "480p",
                            format = "mp4",
                            size = 9000000L
                        )
                    )
                } else {
                    emptyList()
                }

                DownloadQualityDialog(
                    show = showDownloadDialog,
                    movieTitle = "${currentItem.title} - Ep ${currentItem.episode}",
                    currentDubLabel = "Original",
                    availableStreams = streamsForDownload,
                    onDismissRequest = { showDownloadDialog = false },
                    onDownloadConfirmed = { quality, url ->
                        val finalUrl = url.ifBlank {
                            resolvedStreamUrl.ifBlank {
                                currentEpisodeStreams.firstOrNull()?.url ?: currentItem.movie.directUrl
                            }
                        }
                        if (finalUrl.isNotBlank()) {
                            val downloadManager = MovieDownloadManager.getInstance(context)
                            downloadManager.startDownload(
                                movie = currentItem.movie.copy(
                                    title = "${currentItem.title} Ep ${currentItem.episode}"
                                ),
                                quality = quality,
                                downloadUrl = finalUrl,
                                dubLabel = "Original",
                                seasonNumber = currentItem.season,
                                episodeNumber = currentItem.episode,
                                isSeries = true
                            )
                            Toast.makeText(
                                context,
                                "Download started: Ep ${currentItem.episode} ($quality)",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Episode video not ready yet",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDownloadDialog = false
                    }
                )
            }
        }

        // CHOOSE EPISODE BOTTOM SHEET MODAL (Grid type)
        if (showChooseEpisodeModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showChooseEpisodeModal = false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color(0xFF18181B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* prevent dismiss when clicking modal sheet */ }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "All Episodes",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${safeList.size} Episodes available",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(
                                onClick = { showChooseEpisodeModal = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Range Selector (for series with more than 25 episodes)
                        val totalPages = (safeList.size + 24) / 25
                        var selectedRangeIndex by remember {
                            mutableStateOf((pagerState.currentPage / 25).coerceIn(0, (totalPages - 1).coerceAtLeast(0)))
                        }

                        if (totalPages > 1) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(totalPages) { rIdx ->
                                    val startEp = rIdx * 25 + 1
                                    val endEp = minOf((rIdx + 1) * 25, safeList.size)
                                    val isSelected = selectedRangeIndex == rIdx
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) MovieBoxRed else Color.White.copy(alpha = 0.08f),
                                        modifier = Modifier.clickable { selectedRangeIndex = rIdx }
                                    ) {
                                        Text(
                                            text = "$startEp-$endEp",
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 5-Column Grid of Episodes
                        val startEpisodeIndex = if (totalPages > 1) selectedRangeIndex * 25 else 0
                        val endEpisodeIndex = if (totalPages > 1) minOf((selectedRangeIndex + 1) * 25, safeList.size) else safeList.size
                        val episodeSublist = safeList.subList(startEpisodeIndex, endEpisodeIndex)

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            itemsIndexed(episodeSublist) { localIdx, epItem ->
                                val globalIdx = startEpisodeIndex + localIdx
                                val isCurrentPlaying = globalIdx == pagerState.currentPage

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrentPlaying) MovieBoxRed else Color(0xFF27272A),
                                    modifier = Modifier
                                        .height(46.dp)
                                        .clickable {
                                            scope.launch {
                                                pagerState.scrollToPage(globalIdx)
                                            }
                                            showChooseEpisodeModal = false
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = epItem.episodeFormatted.toIntOrNull()?.toString() ?: epItem.episodeFormatted,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
