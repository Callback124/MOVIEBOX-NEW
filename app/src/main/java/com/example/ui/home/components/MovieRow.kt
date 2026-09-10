package com.example.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategorySection
import com.example.data.model.MovieItem
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MovieBoxGold
import com.example.ui.theme.MovieBoxRed

@Composable
fun MovieRow(
    section: CategorySection,
    onMovieClick: (MovieItem) -> Unit,
    onViewMoreClick: (CategorySection) -> Unit = {},
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    if (section.items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("movie_section_${section.id}")
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Red decorative accent bar
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MovieBoxRed)
                )

                Text(
                    text = section.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // "View More" button at the end of the row title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onViewMoreClick(section) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("view_more_${section.id}")
            ) {
                Text(
                    text = "View More",
                    color = MovieBoxRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View More",
                    tint = MovieBoxRed,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Horizontal List of Cards (Landscape or Portrait based on isLandscape)
        val isShortSection = section.isHotShortTvSection

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(section.items, key = { it.id.ifBlank { it.title } }) { movie ->
                if (isLandscape) {
                    MovieLandscapeCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        forceShortsTag = isShortSection
                    )
                } else {
                    MovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie) },
                        forceShortsTag = isShortSection
                    )
                }
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useFixedDimension: Boolean = true,
    forceShortsTag: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "card_scale"
    )

    // Standard cinema portrait poster dimensions (increased height according to portrait movie poster ratio)
    val cardWidth = if (movie.isShort) 136.dp else 130.dp
    val cardHeight = if (movie.isShort) 220.dp else 200.dp

    val baseModifier = if (useFixedDimension) {
        modifier.width(cardWidth)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = baseModifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("movie_card_${movie.id}")
    ) {
        // Poster Card with 5px border radius, NO border & NO border color
        Card(
            shape = RoundedCornerShape(5.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = null,
            modifier = if (useFixedDimension) {
                Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = movie.coverUrl.ifBlank { movie.backdropUrl },
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Corner Tag (e.g. "HD", "English", "Hot")
                if (movie.corner.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 5.dp, topStart = 5.dp),
                        color = MovieBoxRed,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = movie.corner.take(8).uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Short Video Tag if applicable
                val hasShortsTag = forceShortsTag ||
                    (movie.source.equals("ugc-anime.com", ignoreCase = true) && movie.uploadBy.equals("MiniTV", ignoreCase = true)) ||
                    movie.isShortsContent ||
                    movie.isShort
                if (hasShortsTag) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xDD000000),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MovieBoxRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "SHORTS",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // IMDb Rating Badge (Top Right)
                if (movie.rating.isNotBlank() && movie.rating != "0") {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 5.dp, topEnd = 5.dp),
                        color = Color(0xCC000000),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MovieBoxGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = movie.rating,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = movie.title,
            color = Color(0xFFF4F4F5),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle (Release Year + Primary Genre or Duration)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (movie.releaseYear.isNotBlank()) {
                Text(
                    text = movie.releaseYear,
                    color = Color(0xFF71717A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            if (movie.releaseYear.isNotBlank() && movie.genre.isNotBlank()) {
                Text(
                    text = "•",
                    color = Color(0xFF52525B),
                    fontSize = 11.sp
                )
            }
            if (movie.genre.isNotBlank()) {
                Text(
                    text = movie.genre.split(",").firstOrNull() ?: "",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MovieLandscapeCard(
    movie: MovieItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useFixedDimension: Boolean = true,
    forceShortsTag: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "landscape_card_scale"
    )

    val baseModifier = if (useFixedDimension) {
        modifier.width(210.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = baseModifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("movie_landscape_card_${movie.id}")
    ) {
        // Landscape 16:9 widescreen card - 5px radius, NO border & NO border color
        Card(
            shape = RoundedCornerShape(5.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = null,
            modifier = if (useFixedDimension) {
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = movie.backdropUrl.ifBlank { movie.coverUrl },
                    contentDescription = movie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Corner Tag (e.g. "HD", "New", "Hot")
                if (movie.corner.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 5.dp, topStart = 5.dp),
                        color = MovieBoxRed,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = movie.corner.take(8).uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Short Video Tag if applicable
                val hasShortsTag = forceShortsTag ||
                    (movie.source.equals("ugc-anime.com", ignoreCase = true) && movie.uploadBy.equals("MiniTV", ignoreCase = true)) ||
                    movie.isShortsContent ||
                    movie.isShort
                if (hasShortsTag) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xDD000000),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MovieBoxRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "SHORTS",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // IMDb Rating Badge (Top Right)
                if (movie.rating.isNotBlank() && movie.rating != "0") {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 5.dp, topEnd = 5.dp),
                        color = Color(0xCC000000),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MovieBoxGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = movie.rating,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Play icon pill at center
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x99000000),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(6.dp)
                    )
                }

                // Duration / Tag in bottom right if available
                if (movie.duration.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xBB000000),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = movie.duration,
                            color = Color(0xFFE4E4E7),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = movie.title,
            color = Color(0xFFF4F4F5),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Subtitle (Release Year + Genre)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (movie.releaseYear.isNotBlank()) {
                Text(
                    text = movie.releaseYear,
                    color = Color(0xFF71717A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            if (movie.releaseYear.isNotBlank() && movie.genre.isNotBlank()) {
                Text(
                    text = "•",
                    color = Color(0xFF52525B),
                    fontSize = 11.sp
                )
            }
            if (movie.genre.isNotBlank()) {
                Text(
                    text = movie.genre.split(",").firstOrNull() ?: "",
                    color = Color(0xFFA1A1AA),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

