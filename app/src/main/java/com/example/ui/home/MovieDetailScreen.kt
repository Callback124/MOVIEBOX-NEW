package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.R
import com.example.data.api.MovieBoxApiClient
import com.example.data.model.MovieItem
import com.example.data.model.SubjectDetailResult
import com.example.ui.home.components.DetailVideoPlayer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MovieBoxGold
import com.example.ui.theme.MovieBoxRed

@Composable
fun MovieDetailScreen(
    movie: MovieItem,
    playlist: List<MovieItem> = emptyList(),
    isFromShortsPage: Boolean = false,
    isDownloaded: Boolean = false,
    downloadedSeason: Int = 0,
    downloadedEpisode: Int = 0,
    isInWatchlist: Boolean = false,
    onBackClick: () -> Unit,
    onToggleWatchlist: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeMovie by remember(movie) { mutableStateOf(movie) }
    var isFullscreen by remember { mutableStateOf(false) }
    val detailScrollState = rememberScrollState()

    // Intercept back key: if fullscreen, exit fullscreen; otherwise navigate back
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }
    BackHandler(enabled = !isFullscreen) {
        onBackClick()
    }

    val context = LocalContext.current

    // Quality state managed inside player
    var selectedQuality by remember { mutableStateOf("1080p") }

    // Dynamic detail fetch state from MovieBox API
    var detailedInfo by remember(activeMovie.id, activeMovie.detailPath) { mutableStateOf<SubjectDetailResult?>(null) }

    LaunchedEffect(activeMovie.detailPath, activeMovie.id, isDownloaded) {
        if (!isDownloaded && (activeMovie.detailPath.isNotBlank() || activeMovie.id.isNotBlank())) {
            val res = MovieBoxApiClient.fetchSubjectDetail(
                context = context,
                detailPath = activeMovie.detailPath,
                fallbackSubjectId = activeMovie.id
            )
            if (res != null) {
                detailedInfo = res
            }
        }
    }

    // Determine if content is shorts: Strictly ONLY if launched from the Shorts page or Home 1st row!
    val isShortsContent = remember(isFromShortsPage) {
        isFromShortsPage
    }

    val shortsPlaylist = remember(playlist, activeMovie) {
        if (playlist.isNotEmpty()) playlist else listOf(activeMovie)
    }

    // Determine if content is a series or a movie
    val isSeries = remember(activeMovie, detailedInfo) {
        if (detailedInfo != null) {
            detailedInfo!!.isSeries
        } else {
            val genre = activeMovie.genre
            val corner = activeMovie.corner
            val detailPath = activeMovie.detailPath
            when {
                corner.contains("EP", ignoreCase = true) || corner.contains("Season", ignoreCase = true) -> true
                detailPath.contains("season", ignoreCase = true) || detailPath.contains("episode", ignoreCase = true) -> true
                genre.contains("Series", ignoreCase = true) ||
                    genre.contains("TV Series", ignoreCase = true) ||
                    genre.contains("TV Show", ignoreCase = true) ||
                    genre.contains("Anime", ignoreCase = true) -> true
                activeMovie.isShort -> true
                genre.contains("Movie", ignoreCase = true) -> false
                activeMovie.duration.isNotBlank() && (activeMovie.duration.contains("h") || activeMovie.duration.contains("m")) -> false
                else -> false
            }
        }
    }

    // Season list
    val availableSeasons: List<Int> = remember(detailedInfo, isSeries) {
        if (!isSeries) {
            listOf(0)
        } else if (detailedInfo != null && detailedInfo!!.seasons.isNotEmpty()) {
            detailedInfo!!.seasons.map { it.seasonNumber }
        } else {
            listOf(1, 2, 3)
        }
    }

    var selectedSeason by remember(activeMovie) {
        mutableIntStateOf(
            if (isDownloaded && downloadedSeason > 0) downloadedSeason
            else if (isSeries) 1 else 0
        )
    }
    var showSeasonDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(availableSeasons, isSeries) {
        if (isSeries) {
            if (isDownloaded && downloadedSeason > 0) {
                selectedSeason = downloadedSeason
            } else if (availableSeasons.isNotEmpty() && !availableSeasons.contains(selectedSeason)) {
                selectedSeason = availableSeasons.first()
            }
        } else {
            selectedSeason = 0
        }
    }

    // Episode list
    val episodes = remember(selectedSeason, isSeries, detailedInfo) {
        if (!isSeries) {
            emptyList()
        } else {
            val maxEp = detailedInfo?.seasons?.find { it.seasonNumber == selectedSeason }?.maxEp ?: 12
            (1..maxEp).map { String.format("%02d", it) }
        }
    }
    var selectedEpisode by remember(activeMovie) {
        mutableStateOf(
            if (isDownloaded && downloadedEpisode > 0) String.format("%02d", downloadedEpisode)
            else if (isSeries) "01" else "00"
        )
    }

    LaunchedEffect(episodes, isSeries) {
        if (isSeries) {
            if (isDownloaded && downloadedEpisode > 0) {
                selectedEpisode = String.format("%02d", downloadedEpisode)
            } else if (episodes.isNotEmpty() && !episodes.contains(selectedEpisode)) {
                selectedEpisode = episodes.first()
            }
        } else {
            selectedEpisode = "00"
        }
    }

    // Effective MovieItem combining initial item + detailed metadata
    val effectiveMovie = remember(activeMovie, detailedInfo) {
        if (detailedInfo != null) {
            activeMovie.copy(
                id = detailedInfo!!.subjectId.ifBlank { activeMovie.id },
                title = detailedInfo!!.title.ifBlank { activeMovie.title },
                description = detailedInfo!!.description.ifBlank { activeMovie.description },
                coverUrl = detailedInfo!!.coverUrl.ifBlank { activeMovie.coverUrl },
                backdropUrl = detailedInfo!!.backdropUrl.ifBlank { activeMovie.backdropUrl },
                rating = detailedInfo!!.rating.ifBlank { activeMovie.rating },
                ratingCount = if (detailedInfo!!.ratingCount > 0) detailedInfo!!.ratingCount else activeMovie.ratingCount,
                releaseDate = detailedInfo!!.releaseDate.ifBlank { activeMovie.releaseDate },
                releaseYear = detailedInfo!!.releaseYear.ifBlank { activeMovie.releaseYear },
                genre = detailedInfo!!.genre.ifBlank { activeMovie.genre },
                country = detailedInfo!!.country.ifBlank { activeMovie.country },
                duration = detailedInfo!!.durationFormatted.ifBlank { activeMovie.duration },
                detailPath = detailedInfo!!.detailPath.ifBlank { activeMovie.detailPath }
            )
        } else {
            activeMovie
        }
    }

    // Derived metadata fields
    val displayTitle = detailedInfo?.title?.ifBlank { activeMovie.title } ?: activeMovie.title
    val displayDescription = detailedInfo?.description?.ifBlank { activeMovie.description } ?: activeMovie.description
    val displayRating = detailedInfo?.rating?.ifBlank { activeMovie.rating.ifBlank { "7.8" } } ?: activeMovie.rating.ifBlank { "7.8" }
    val displayYear = detailedInfo?.releaseYear?.ifBlank {
        activeMovie.releaseYear.ifBlank { activeMovie.releaseDate.take(4).ifBlank { "2024" } }
    } ?: "2024"
    val displayCountry = detailedInfo?.country?.ifBlank { activeMovie.country.ifBlank { "United States" } } ?: "United States"

    // Main View (NO back button on detail page)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullscreen) Color.Black else DarkBackground)
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
                    .testTag("movie_detail_page_screen")
            ) {
                // 1. ORIGINAL ART PLAYER (WITHOUT BACK BUTTON, QUALITY SWITCHER INSIDE PLAYER)
                // Persistent instance prevents reloading / re-fetching during orientation & fullscreen changes
                val isLastEpisodeOfLastSeason = remember(isSeries, selectedSeason, selectedEpisode, episodes, availableSeasons) {
                    if (!isSeries) true
                    else {
                        val currentEpNum = selectedEpisode.toIntOrNull() ?: 1
                        val currentIndex = episodes.indexOfFirst { (it.toIntOrNull() ?: 0) == currentEpNum }
                        val isLastEpInSeason = currentIndex == -1 || currentIndex + 1 >= episodes.size
                        val currentSeasonIndex = availableSeasons.indexOf(selectedSeason)
                        val isLastSeason = currentSeasonIndex == -1 || currentSeasonIndex + 1 >= availableSeasons.size
                        isLastEpInSeason && isLastSeason
                    }
                }

                val playNextEpisode = remember(isSeries, episodes, selectedEpisode, availableSeasons, selectedSeason) {
                    {
                        if (isSeries && episodes.isNotEmpty()) {
                            val currentEpNum = selectedEpisode.toIntOrNull() ?: 1
                            val currentIndex = episodes.indexOfFirst { (it.toIntOrNull() ?: 0) == currentEpNum }
                            if (currentIndex != -1 && currentIndex + 1 < episodes.size) {
                                selectedEpisode = episodes[currentIndex + 1]
                            } else {
                                val currentSeasonIndex = availableSeasons.indexOf(selectedSeason)
                                if (currentSeasonIndex != -1 && currentSeasonIndex + 1 < availableSeasons.size) {
                                    selectedSeason = availableSeasons[currentSeasonIndex + 1]
                                    selectedEpisode = "01"
                                }
                            }
                        }
                    }
                }

                DetailVideoPlayer(
                    movie = effectiveMovie,
                    isSeries = isSeries,
                    selectedSeason = if (isSeries) selectedSeason else 0,
                    selectedEpisode = if (isSeries) (selectedEpisode.toIntOrNull() ?: 1) else 0,
                    selectedQuality = selectedQuality,
                    dubs = detailedInfo?.dubs?.takeIf { it.isNotEmpty() } ?: effectiveMovie.dubs,
                    onQualitySelected = { q -> selectedQuality = q },
                    isFullscreen = isFullscreen,
                    onFullscreenChanged = { fs -> isFullscreen = fs },
                    isFromShortsPage = isShortsContent,
                    isDownloaded = isDownloaded,
                    isLastEpisodeOfLastSeason = isLastEpisodeOfLastSeason,
                    onPlayNext = playNextEpisode,
                    onPlaybackEnded = playNextEpisode,
                    modifier = if (isFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    }
                )

            // 2. DETAIL SECTION (BELOW PLAYER) - Visible in portrait / non-fullscreen mode
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(detailScrollState)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // Top Row: Left Thumbnail + Right (Title + Other Details)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left: Thumbnail with 5.dp rounded corners, no border
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier
                                .width(92.dp)
                                .height(138.dp)
                        ) {
                        AsyncImage(
                            model = effectiveMovie.coverUrl.ifBlank { effectiveMovie.backdropUrl },
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Right: Title & Other Details (ONLY Type Icon, Star, Year, Country)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    ) {
                        // Title
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier = Modifier.testTag("detail_title_text")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Other Details Row: SIRF Type Icon, Star, Year, Country
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Type Icon (Movies / TV Series SVG icon provided by user)
                            Icon(
                                painter = painterResource(
                                    id = if (isSeries) R.drawable.ic_type_series else R.drawable.ic_type_movie
                                ),
                                contentDescription = if (isSeries) "TV Series" else "Movie",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp)
                            )

                            Text(text = "|", color = Color(0xFF52525B), fontSize = 11.sp)

                            // Star Rating
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = MovieBoxGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = displayRating,
                                color = MovieBoxGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(text = "|", color = Color(0xFF52525B), fontSize = 11.sp)

                            // Year
                            Text(
                                text = displayYear,
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp
                            )

                            Text(text = "|", color = Color(0xFF52525B), fontSize = 11.sp)

                            // Country
                            Text(
                                text = displayCountry,
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thumbnail & detail ke neeche synopsis hoga
                Text(
                    text = "Synopsis",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = displayDescription.ifBlank { "No detailed synopsis available." },
                    color = Color(0xFFD4D4D8),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )

                // Cast & Crew (if available, skipped when offline downloaded)
                if (!isDownloaded && detailedInfo?.cast?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cast & Crew",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(detailedInfo!!.cast.take(8)) { actor ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DarkSurfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    if (actor.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = actor.avatarUrl,
                                            contentDescription = actor.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = actor.name,
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. BOTTOM SECTION: SEASONS & EPISODES (Only for online series, skipped for downloaded offline video)
            if (!isDownloaded && isSeries) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141418))
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Season Selector Row (Border radius 10px)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = Color(0xFF22222A),
                                modifier = Modifier
                                    .clickable { showSeasonDropdown = true }
                                    .testTag("season_selector_box")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = String.format("Season %02d", selectedSeason),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Choose Season",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSeasonDropdown,
                                onDismissRequest = { showSeasonDropdown = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                availableSeasons.forEach { sNum ->
                                    val sLabel = String.format("Season %02d", sNum)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = sLabel,
                                                color = if (sNum == selectedSeason) MovieBoxRed else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (sNum == selectedSeason) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedSeason = sNum
                                            showSeasonDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Episodes Selector Row (Card border radius 5px, no border)
                    if (episodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("episodes_lazy_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(episodes) { ep ->
                                val isSelected = (ep == selectedEpisode)
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (isSelected) MovieBoxRed else Color(0xFF22222A),
                                    modifier = Modifier
                                        .clickable { selectedEpisode = ep }
                                        .width(46.dp)
                                        .height(36.dp)
                                        .testTag("episode_card_$ep")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ep,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
}
