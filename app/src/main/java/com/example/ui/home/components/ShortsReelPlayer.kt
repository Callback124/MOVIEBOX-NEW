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
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import android.net.Uri
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
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
private fun buildShortMediaSource(context: Context, streamUrl: String, format: String): MediaSource {
    val isLocal = streamUrl.startsWith("/") || streamUrl.startsWith("file:")
    val uri = if (streamUrl.startsWith("/")) Uri.fromFile(java.io.File(streamUrl)) else Uri.parse(streamUrl)

    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36")
        .setDefaultRequestProperties(
            mapOf(
                "Referer" to "https://movieboxph.org/",
                "Origin" to "https://movieboxph.org",
                "Accept" to "*/*"
            )
        )
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)

    val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
    val mediaItem = MediaItem.fromUri(uri)
    val upper = format.uppercase()
    return when {
        !isLocal && (upper.contains("M3U8") || upper.contains("HLS") || streamUrl.contains(".m3u8")) -> {
            HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        }
        !isLocal && (upper.contains("MPD") || upper.contains("DASH") || streamUrl.contains(".mpd")) -> {
            DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
        else -> {
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }
}

private val LANGUAGE_MAP = mapOf(
    "en" to "English",
    "eng" to "English",
    "hi" to "Hindi",
    "hin" to "Hindi",
    "es" to "Spanish",
    "spa" to "Spanish",
    "fr" to "French",
    "fra" to "French",
    "fre" to "French",
    "de" to "German",
    "deu" to "German",
    "ger" to "German",
    "it" to "Italian",
    "ita" to "Italian",
    "pt" to "Portuguese",
    "por" to "Portuguese",
    "ru" to "Russian",
    "rus" to "Russian",
    "ja" to "Japanese",
    "jpn" to "Japanese",
    "ko" to "Korean",
    "kor" to "Korean",
    "zh" to "Chinese",
    "chi" to "Chinese",
    "zho" to "Chinese",
    "cmn" to "Mandarin",
    "yue" to "Cantonese",
    "ta" to "Tamil",
    "tam" to "Tamil",
    "te" to "Telugu",
    "tel" to "Telugu",
    "th" to "Thai",
    "tha" to "Thai",
    "vi" to "Vietnamese",
    "vie" to "Vietnamese",
    "id" to "Indonesian",
    "ind" to "Indonesian",
    "tr" to "Turkish",
    "tur" to "Turkish",
    "ar" to "Arabic",
    "ara" to "Arabic",
    "fil" to "Filipino",
    "tl" to "Tagalog"
)

private fun resolveDubLabel(lanName: String, lanCode: String, isOriginal: Boolean): String {
    val cleanName = lanName.trim()
    val cleanCode = lanCode.trim().lowercase()
    val mapped = LANGUAGE_MAP[cleanCode] ?: LANGUAGE_MAP[cleanName.lowercase()]
    val base = when {
        cleanName.isNotBlank() && cleanName.length > 2 -> cleanName
        mapped != null -> mapped
        cleanCode.isNotBlank() -> cleanCode.uppercase()
        else -> "Original"
    }
    return if (isOriginal) {
        if (base.contains("Original", ignoreCase = true)) base else "$base (Original)"
    } else {
        if (base.contains("Dub", ignoreCase = true)) base else "$base Dub"
    }
}

private fun formatResolutionDisplay(cleanRes: String): String {
    val num = cleanRes.filter { it.isDigit() }
    return when (num) {
        "2160" -> "3840 × 2160"
        "1440" -> "2560 × 1440"
        "1080" -> "1920 × 1080"
        "720" -> "1280 × 720"
        "480" -> "854 × 480"
        "360" -> "640 × 360"
        else -> if (num.isNotBlank()) "${num}p" else cleanRes
    }
}

private fun extractResolutionHeight(resText: String): String {
    val clean = resText.trim()
    if (clean.contains("×")) {
        return clean.split("×").lastOrNull()?.trim()?.filter { it.isDigit() } ?: ""
    }
    return clean.filter { it.isDigit() }
}

private fun getSavedQualityPreference(context: Context): String {
    return try {
        val sp = context.getSharedPreferences("player_settings_pref", Context.MODE_PRIVATE)
        sp.getString("saved_video_quality", "1920 × 1080") ?: "1920 × 1080"
    } catch (_: Exception) {
        "1920 × 1080"
    }
}

private fun saveQualityPreference(context: Context, quality: String) {
    try {
        val sp = context.getSharedPreferences("player_settings_pref", Context.MODE_PRIVATE)
        sp.edit().putString("saved_video_quality", quality).apply()
    } catch (_: Exception) {}
}

data class CachedShortStream(
    val streamUrl: String,
    val format: String,
    val streams: List<MovieStream>,
    val qualities: List<String>,
    val selectedQuality: String
)

data class ReelEpisodeItem(
    val id: String,
    val movie: MovieItem,
    val season: Int,
    val episode: Int,
    val episodeFormatted: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val directStreamUrl: String = "",
    val directStreams: List<MovieStream> = emptyList(),
    val defaultResolution: String = ""
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
    var currentDubs by remember(movie.id, detailedInfo) {
        mutableStateOf(detailedInfo?.dubs?.ifEmpty { null } ?: movie.dubs)
    }
    var activePlayingStreamUrl by remember { mutableStateOf("") }

    LaunchedEffect(movie.id, movie.detailPath, movie.isVskitServer) {
        val isVskit = movie.isVskitServer || movie.source.equals("vskit", ignoreCase = true) ||
            movie.uploadBy.equals("ShortsTV", ignoreCase = true) || movie.customHeaders.containsKey("X-Site-Domain")
        val subjectId = movie.id.ifBlank { movie.detailPath }
        if (isVskit) {
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
                if (res.dubs.isNotEmpty()) {
                    currentDubs = res.dubs
                }
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
                ?: detailedInfo?.seasons?.maxOfOrNull { it.maxEp }
                ?: detailedInfo?.seasons?.firstOrNull()?.maxEp
            if (fromDetail != null && fromDetail > 1) {
                (1..fromDetail).map { String.format("%02d", it) }
            } else if (movie.totalEpisodes > 1) {
                (1..movie.totalEpisodes).map { String.format("%02d", it) }
            } else {
                val cornerEp = Regex("""(\d+)\s*(?:EP|ep|Episodes|Ep)""").find(movie.corner)?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("""(\d+)""").find(movie.corner)?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("""(\d+)""").find(detailedInfo?.corner ?: "")?.groupValues?.get(1)?.toIntOrNull()
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
                directStreamUrl = vskitItem?.videoUrl ?: "",
                directStreams = vskitItem?.streams ?: emptyList(),
                defaultResolution = vskitItem?.resolution ?: ""
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
    val streamCache = remember { mutableMapOf<String, CachedShortStream>() }
    var userManualQualityOverride by remember { mutableStateOf<String?>(null) }
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
    var selectedQuality by remember { mutableStateOf("") }
    var availableVideoQualities by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentEpisodeStreams by remember { mutableStateOf<List<MovieStream>>(emptyList()) }
    val internalAudioTracks = remember { mutableStateListOf<PlayerAudioTrack>() }

    val availableAudioTracks = remember(currentDubs, internalAudioTracks) {
        val list = mutableListOf<PlayerAudioTrack>()
        if (currentDubs.isNotEmpty()) {
            for (dub in currentDubs) {
                val label = resolveDubLabel(dub.lanName, dub.lanCode, dub.original)
                val safeSubjId = dub.subjectId.ifBlank { dub.lanCode.ifBlank { dub.lanName } }
                val effectiveSubjId = dub.subjectId.ifBlank {
                    if (dub.original) movie.id else ""
                }
                list.add(
                    PlayerAudioTrack(
                        id = "dub_${safeSubjId}_${dub.lanCode}",
                        label = label,
                        isDub = true,
                        dubSubjectId = effectiveSubjId,
                        dubDetailPath = dub.detailPath
                    )
                )
            }
        }
        for (internal in internalAudioTracks) {
            if (internal.id != "auto" && internal.id != "default") {
                list.add(internal)
            }
        }
        if (list.isEmpty()) {
            val defaultLabel = if (movie.corner.isNotBlank() && (movie.corner.contains("Dub", ignoreCase = true) || movie.corner.contains("Eng", ignoreCase = true) || movie.corner.contains("Hin", ignoreCase = true))) {
                "${movie.corner} (Original)"
            } else {
                "Original Audio"
            }
            list.add(PlayerAudioTrack(id = "auto", label = defaultLabel))
        }
        list
    }
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
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
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
        val currentTracks = exoPlayer.currentTracks
        for (i in 0 until currentTracks.groups.size) {
            val group = currentTracks.groups[i]
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (j in 0 until group.length) {
                    val format = group.getTrackFormat(j)
                    val lang = format.language?.lowercase() ?: ""
                    val mapped = LANGUAGE_MAP[lang]
                    val label = when {
                        !format.label.isNullOrBlank() -> format.label!!
                        mapped != null -> "$mapped (Audio)"
                        lang.isNotBlank() -> "${lang.uppercase()} (Audio)"
                        else -> "Track ${j + 1}"
                    }
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
        internalAudioTracks.clear()
        internalAudioTracks.addAll(tracks)
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
                        currentPositionMs = if (durationMs > 0) durationMs else exoPlayer.duration.coerceAtLeast(0L)
                        // AUTO PLAY NEXT EPISODE WITH SMOOTH SCROLL ANIMATION (Reel Style!)
                        scope.launch {
                            val nextIndex = pagerState.currentPage + 1
                            if (nextIndex < safeList.size) {
                                pagerState.animateScrollToPage(nextIndex)
                            } else {
                                // Last episode completed: pause shorts player, matching MovieBox's player behavior
                                isPlaying = false
                                exoPlayer.pause()
                                areControlsVisible = true
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
                        val fallback = buildShortMediaSource(context, FALLBACK_SHORT_STREAM, "MP4")
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
                        val isVskitItem = item.movie.isVskitServer ||
                            item.movie.source.equals("vskit", ignoreCase = true) ||
                            item.movie.uploadBy.equals("ShortsTV", ignoreCase = true) ||
                            item.directStreams.isNotEmpty()
                        val streams = if (item.directStreams.isNotEmpty()) {
                            item.directStreams.filter { it.url.isNotBlank() }
                        } else if (isVskitItem) {
                            val eps = VskitShortsApiClient.fetchShortsEpisodes(item.movie.id)
                            val match = eps.find { it.ep == item.episode }
                            if (match != null && match.streams.isNotEmpty()) {
                                match.streams.filter { it.url.isNotBlank() }
                            } else if (match != null && match.videoUrl.isNotBlank()) {
                                val cleanRes = match.resolution.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "720" } ?: "720"
                                val fmt = if (match.videoUrl.contains(".m3u8")) "HLS" else "MP4"
                                listOf(MovieStream(id = "vskit_${match.ep}", resolution = cleanRes, format = fmt, url = match.videoUrl))
                            } else emptyList()
                        } else {
                            val res = MovieBoxApiClient.fetchPlayStreams(
                                context = context,
                                subjectId = item.movie.id,
                                detailPath = item.movie.detailPath,
                                isShort = true,
                                season = item.season,
                                episode = item.episode,
                                customHeaders = item.movie.customHeaders
                            )
                            res.streams.filter { it.url.isNotBlank() }
                        }

                        if (streams.isNotEmpty()) {
                            val highest = streams.maxByOrNull { extractResolutionHeight(it.resolution).toIntOrNull() ?: 0 } ?: streams.firstOrNull()
                            val qualities = streams.map { formatResolutionDisplay(it.resolution) }.distinct().sortedByDescending { extractResolutionHeight(it).toIntOrNull() ?: 0 }
                            val bestQuality = highest?.let { formatResolutionDisplay(it.resolution) } ?: qualities.firstOrNull() ?: "1280 × 720"
                            if (highest != null && highest.url.isNotBlank()) {
                                streamCache[cacheKey] = CachedShortStream(highest.url, highest.format, streams, qualities, bestQuality)
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

        val activeDub = availableAudioTracks.find { it.id == currentAudioTrackId && it.isDub }
        val targetSubjectId = activeDub?.dubSubjectId?.takeIf { it.isNotBlank() } ?: currentItem.movie.id
        val targetDetailPath = activeDub?.dubDetailPath ?: currentItem.movie.detailPath

        val isVskit = currentItem.movie.isVskitServer ||
            currentItem.movie.source.equals("vskit", ignoreCase = true) ||
            currentItem.movie.uploadBy.equals("ShortsTV", ignoreCase = true) ||
            currentItem.movie.customHeaders.containsKey("X-Site-Domain") ||
            vskitEpisodes.isNotEmpty() ||
            currentItem.directStreams.isNotEmpty()

        val cacheKey = "${targetSubjectId}_${currentItem.season}_${currentItem.episode}"
        val cached = streamCache[cacheKey]

        val streamUrl: String
        val format: String

        if (cached != null && cached.streamUrl.isNotBlank()) {
            currentEpisodeStreams = cached.streams
            availableVideoQualities = cached.qualities
            if (userManualQualityOverride != null && cached.qualities.contains(userManualQualityOverride)) {
                selectedQuality = userManualQualityOverride!!
                val digits = extractResolutionHeight(userManualQualityOverride!!)
                val match = cached.streams.firstOrNull { extractResolutionHeight(it.resolution) == digits }
                streamUrl = match?.url ?: cached.streamUrl
                format = match?.format ?: cached.format
            } else {
                selectedQuality = cached.selectedQuality
                streamUrl = cached.streamUrl
                format = cached.format
            }
            isFetchingStream = false
        } else if (isVskit && activeDub == null) {
            val resolvedStreams = mutableListOf<MovieStream>()

            // 1. First attempt to use directStreams or pre-fetched vskit episodes
            val vskitMatch = vskitEpisodes.find { it.ep == currentItem.episode }
            if (vskitMatch != null && vskitMatch.streams.isNotEmpty()) {
                resolvedStreams.addAll(vskitMatch.streams.filter { it.url.isNotBlank() })
            } else if (currentItem.directStreams.isNotEmpty()) {
                resolvedStreams.addAll(currentItem.directStreams.filter { it.url.isNotBlank() })
            }

            // 2. Play API with custom headers
            if (resolvedStreams.isEmpty()) {
                val vskitPlayResult = MovieBoxApiClient.fetchPlayStreams(
                    context = context,
                    subjectId = targetSubjectId,
                    detailPath = targetDetailPath,
                    isShort = true,
                    season = currentItem.season,
                    episode = currentItem.episode,
                    customHeaders = currentItem.movie.customHeaders
                )
                if (vskitPlayResult.streams.isNotEmpty()) {
                    resolvedStreams.addAll(vskitPlayResult.streams.filter { it.url.isNotBlank() })
                }
            }

            // 3. Fall back to VskitShortsApiClient.fetchShortsEpisodes
            if (resolvedStreams.isEmpty()) {
                val eps = if (vskitEpisodes.isNotEmpty()) vskitEpisodes else VskitShortsApiClient.fetchShortsEpisodes(currentItem.movie.id)
                val match = eps.find { it.ep == currentItem.episode } ?: eps.firstOrNull()
                if (match != null) {
                    if (match.streams.isNotEmpty()) {
                        resolvedStreams.addAll(match.streams.filter { it.url.isNotBlank() })
                    } else if (match.videoUrl.isNotBlank()) {
                        val cleanRes = match.resolution.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "720" } ?: "720"
                        val fmt = if (match.videoUrl.contains(".m3u8")) "HLS" else "MP4"
                        resolvedStreams.add(MovieStream(id = "vskit_${match.ep}", resolution = cleanRes, format = fmt, url = match.videoUrl))
                    }
                }
            }

            // 4. Fall back to direct stream url if available
            if (resolvedStreams.isEmpty() && currentItem.directStreamUrl.isNotBlank()) {
                val cleanRes = currentItem.defaultResolution.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "720" } ?: "720"
                val fmt = if (currentItem.directStreamUrl.contains(".m3u8")) "HLS" else "MP4"
                resolvedStreams.add(MovieStream(id = "direct_${currentItem.episode}", resolution = cleanRes, format = fmt, url = currentItem.directStreamUrl))
            }

            currentEpisodeStreams = resolvedStreams

            // VSKIT: ONLY show the FETCHED qualities from resources!
            val streamQualities = resolvedStreams
                .filter { it.url.isNotBlank() }
                .map { s ->
                    val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                    formatResolutionDisplay(clean)
                }
                .distinct()
                .sortedByDescending { q ->
                    extractResolutionHeight(q).toIntOrNull() ?: 0
                }

            availableVideoQualities = if (streamQualities.isNotEmpty()) {
                streamQualities
            } else {
                val fallback = currentItem.defaultResolution.filter { it.isDigit() }.ifBlank { "720" }
                listOf(formatResolutionDisplay(fallback))
            }

            // Requirement: Highest fetched quality is default selected
            val highestStream = resolvedStreams.maxByOrNull { s ->
                extractResolutionHeight(s.resolution).toIntOrNull() ?: 0
            } ?: resolvedStreams.firstOrNull()

            val highestQualityDisplay = highestStream?.let { s ->
                val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                formatResolutionDisplay(clean)
            } ?: availableVideoQualities.firstOrNull() ?: "1280 × 720"

            val chosenStream: MovieStream?
            if (userManualQualityOverride != null && availableVideoQualities.contains(userManualQualityOverride)) {
                val targetDigits = extractResolutionHeight(userManualQualityOverride!!)
                chosenStream = resolvedStreams.firstOrNull { extractResolutionHeight(it.resolution) == targetDigits } ?: highestStream
                selectedQuality = userManualQualityOverride!!
            } else {
                chosenStream = highestStream
                selectedQuality = highestQualityDisplay
            }

            streamUrl = chosenStream?.url?.ifBlank { null } ?: currentItem.directStreamUrl.ifBlank { null } ?: FALLBACK_SHORT_STREAM
            format = chosenStream?.format ?: if (streamUrl.contains(".m3u8")) "HLS" else "MP4"

            if (streamUrl != FALLBACK_SHORT_STREAM) {
                streamCache[cacheKey] = CachedShortStream(streamUrl, format, currentEpisodeStreams, availableVideoQualities, selectedQuality)
            }
            isFetchingStream = false
        } else {
            // MOVIEBOX / NON-VSKIT
            val streamResult = MovieBoxApiClient.fetchPlayStreams(
                context = context,
                subjectId = targetSubjectId,
                detailPath = targetDetailPath,
                isShort = true,
                season = currentItem.season,
                episode = currentItem.episode,
                customHeaders = currentItem.movie.customHeaders
            )
            val resolvedStreams = streamResult.streams.filter { it.url.isNotBlank() }
            currentEpisodeStreams = resolvedStreams

            // MOVIEBOX: show fetched resource qualities
            val streamQualities = resolvedStreams
                .map { s ->
                    val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                    formatResolutionDisplay(clean)
                }
                .distinct()
                .sortedByDescending { q ->
                    extractResolutionHeight(q).toIntOrNull() ?: 0
                }

            availableVideoQualities = if (streamQualities.isNotEmpty()) {
                streamQualities
            } else {
                val fallback = currentItem.defaultResolution.filter { it.isDigit() }.ifBlank { "720" }
                listOf(formatResolutionDisplay(fallback))
            }

            // Requirement: MovieBox also defaults to highest fetched quality!
            val highestStream = resolvedStreams.maxByOrNull { s ->
                extractResolutionHeight(s.resolution).toIntOrNull() ?: 0
            } ?: streamResult.defaultStream ?: resolvedStreams.firstOrNull()

            val highestQualityDisplay = highestStream?.let { s ->
                val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                formatResolutionDisplay(clean)
            } ?: availableVideoQualities.firstOrNull() ?: "1280 × 720"

            val chosenStream: MovieStream?
            if (userManualQualityOverride != null && availableVideoQualities.contains(userManualQualityOverride)) {
                val targetDigits = extractResolutionHeight(userManualQualityOverride!!)
                chosenStream = resolvedStreams.firstOrNull { extractResolutionHeight(it.resolution) == targetDigits } ?: highestStream
                selectedQuality = userManualQualityOverride!!
            } else {
                chosenStream = highestStream
                selectedQuality = highestQualityDisplay
            }

            streamUrl = chosenStream?.url?.ifBlank { null } ?: currentItem.directStreamUrl.ifBlank { null } ?: currentItem.movie.directUrl.ifBlank { null } ?: FALLBACK_SHORT_STREAM
            format = chosenStream?.format ?: if (streamUrl.contains(".m3u8")) "HLS" else "MP4"
            if (streamUrl != FALLBACK_SHORT_STREAM) {
                activePlayingStreamUrl = streamUrl
            }

            if (streamUrl != FALLBACK_SHORT_STREAM) {
                streamCache[cacheKey] = CachedShortStream(streamUrl, format, currentEpisodeStreams, availableVideoQualities, selectedQuality)
            }
            isFetchingStream = false
        }

        try {
            val mediaSource = buildShortMediaSource(context, streamUrl, format)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (_: Exception) {
            val fallback = buildShortMediaSource(context, FALLBACK_SHORT_STREAM, "MP4")
            exoPlayer.setMediaSource(fallback)
            exoPlayer.prepare()
            exoPlayer.play()
        }

        val activeDigits = extractResolutionHeight(selectedQuality)
        if (activeDigits.isNotBlank()) {
            val (targetW, targetH) = when (activeDigits) {
                "2160" -> 3840 to 2160
                "1440" -> 2560 to 1440
                "1080" -> 1920 to 1080
                "720" -> 1280 to 720
                "480" -> 854 to 480
                "360" -> 640 to 360
                else -> 1280 to 720
            }
            try {
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(targetW, targetH)
                    .setMinVideoSize(if (activeDigits == "1080" || activeDigits == "2160") 720 else 0, if (activeDigits == "1080" || activeDigits == "2160") 720 else 0)
                    .build()
            } catch (_: Exception) {}
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
                        Spacer(modifier = Modifier.weight(1f))

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

                            // Subtitle: EP X/Total • Description (Matches original layout with full details)
                            val epNumber = shortItem.episodeFormatted.toIntOrNull() ?: shortItem.episodeFormatted
                            val totalEpCount = safeList.size
                            val detailSynopsis = (detailedInfo?.description?.ifBlank { null }
                                ?: shortItem.description.ifBlank { null }
                                ?: shortItem.movie.description.ifBlank { null }
                                ?: shortItem.subtitle).trim()

                            val subtitleText = buildString {
                                append("EP $epNumber")
                                if (totalEpCount > 1) {
                                    append("/$totalEpCount")
                                }
                                if (detailSynopsis.isNotBlank()) {
                                    append(" • ")
                                    append(detailSynopsis)
                                }
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
                    userManualQualityOverride = newQuality
                    saveQualityPreference(context, newQuality)
                    val digits = extractResolutionHeight(newQuality)
                    val matchedStream = if (digits.isNotBlank()) {
                        currentEpisodeStreams.firstOrNull { s ->
                            val clean = extractResolutionHeight(s.resolution)
                            clean == digits || clean.contains(digits) || digits.contains(clean)
                        }
                    } else null
                    val targetStream = matchedStream ?: currentEpisodeStreams.firstOrNull()
                    if (targetStream != null && targetStream.url.isNotBlank()) {
                        val currentPos = exoPlayer.currentPosition
                        val currentItem = safeList.getOrNull(pagerState.currentPage)
                        if (currentItem != null) {
                            val activeDub = availableAudioTracks.find { it.id == currentAudioTrackId && it.isDub }
                            val targetSubjId = activeDub?.dubSubjectId?.takeIf { it.isNotBlank() } ?: currentItem.movie.id
                            val cKey = "${targetSubjId}_${currentItem.season}_${currentItem.episode}"
                            streamCache[cKey] = CachedShortStream(targetStream.url, targetStream.format, currentEpisodeStreams, availableVideoQualities, newQuality)
                        }
                        val mediaSource = buildShortMediaSource(context, targetStream.url, targetStream.format)
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()
                        if (currentPos > 0) exoPlayer.seekTo(currentPos)
                        exoPlayer.play()
                    }
                    val (targetW, targetH) = when (digits) {
                        "2160" -> 3840 to 2160
                        "1440" -> 2560 to 1440
                        "1080" -> 1920 to 1080
                        "720" -> 1280 to 720
                        "480" -> 854 to 480
                        "360" -> 640 to 360
                        else -> 1280 to 720
                    }
                    try {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoSize(targetW, targetH)
                            .setMinVideoSize(if (digits == "1080" || digits == "2160") 720 else 0, if (digits == "1080" || digits == "2160") 720 else 0)
                            .build()
                    } catch (_: Exception) {}
                },
                availableAudioTracks = availableAudioTracks,
                currentAudioTrackId = currentAudioTrackId,
                onAudioTrackSelected = { selectedAudio ->
                    currentAudioTrackId = selectedAudio.id
                    if (selectedAudio.isDub) {
                        val effectiveSubjId = selectedAudio.dubSubjectId.ifBlank {
                            if (selectedAudio.label.contains("Original", ignoreCase = true)) movie.id else ""
                        }
                        if (effectiveSubjId.isNotBlank() || selectedAudio.dubDetailPath.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val currentItem = safeList.getOrNull(pagerState.currentPage)
                                    val currentSeason = currentItem?.season ?: 1
                                    val currentEp = currentItem?.episode ?: 1
                                    val currentPos = exoPlayer.currentPosition

                                    val dubResult = MovieBoxApiClient.fetchPlayStreams(
                                        context = context,
                                        subjectId = effectiveSubjId,
                                        detailPath = selectedAudio.dubDetailPath,
                                        isShort = true,
                                        season = currentSeason,
                                        episode = currentEp
                                    )
                                    val validStreams = dubResult.streams.filter { it.url.isNotBlank() }
                                    val digits = extractResolutionHeight(selectedQuality)
                                    val matched = validStreams.firstOrNull { s ->
                                        val clean = s.resolution.split(",").firstOrNull()?.filter { it.isDigit() } ?: ""
                                        digits.isNotBlank() && (clean.contains(digits) || digits.contains(clean))
                                    } ?: dubResult.defaultStream?.takeIf { it.url.isNotBlank() } ?: validStreams.firstOrNull()

                                    if (matched != null && matched.url.isNotBlank()) {
                                        currentEpisodeStreams = validStreams
                                        val cacheKey = "${effectiveSubjId}_${currentSeason}_${currentEp}"
                                        val dubQualities = validStreams.map { formatResolutionDisplay(it.resolution) }.distinct().sortedByDescending { extractResolutionHeight(it).toIntOrNull() ?: 0 }
                                        val matchedQuality = formatResolutionDisplay(matched.resolution)
                                        streamCache[cacheKey] = CachedShortStream(matched.url, matched.format, validStreams, dubQualities, matchedQuality)
                                        withContext(Dispatchers.Main) {
                                            val mediaSource = buildShortMediaSource(context, matched.url, matched.format)
                                            exoPlayer.setMediaSource(mediaSource)
                                            exoPlayer.prepare()
                                            if (currentPos > 0) exoPlayer.seekTo(currentPos)
                                            exoPlayer.play()
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Dub stream not available for this episode", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ShortsReelPlayer", "Error loading dub stream", e)
                                }
                            }
                        }
                    } else if (selectedAudio.groupIndex >= 0) {
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
                val resolvedStreamUrl = activePlayingStreamUrl.ifBlank {
                    currentItem.directStreamUrl.ifBlank {
                        vskitItem?.videoUrl ?: ""
                    }
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
