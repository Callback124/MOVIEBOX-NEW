package com.example.ui.home.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HeroBanner
import kotlinx.coroutines.delay

@Composable
fun HeroBannerCarousel(
    banners: List<HeroBanner>,
    onBannerClick: (HeroBanner) -> Unit,
    onPlayClick: ((HeroBanner) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    val virtualPageCount = if (banners.size > 1) 1_000_000 else banners.size
    val initialPage = if (banners.size > 1) 500_000 - (500_000 % banners.size) else 0

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )

    // Auto-advance banner carousel smoothly every 5 seconds (runs continuously right-to-left in an infinite loop)
    LaunchedEffect(banners.size) {
        if (banners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000L)
            // Wait while user is interacting/scrolling
            while (pagerState.isScrollInProgress) {
                delay(800L)
            }
            try {
                pagerState.animateScrollToPage(
                    page = pagerState.currentPage + 1,
                    animationSpec = tween(durationMillis = 650)
                )
            } catch (_: Exception) {
                // If cancelled by touch during animation, loop continues seamlessly
            }
        }
    }

    val actualCurrentIndex = if (banners.isNotEmpty()) {
        (pagerState.currentPage % banners.size).coerceIn(0, banners.size - 1)
    } else 0
    val currentBanner = banners[actualCurrentIndex]

    // Hero Section matching exact landscape thumbnail aspect ratio without any dark shadow/filter
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .testTag("hero_banner_carousel")
    ) {
        // 1. Large Hero Backdrop/Landscape Thumbnail in the background (fades smoothly when page changes, 100% clear with NO dark shadow or filter)
        Crossfade(
            targetState = currentBanner,
            animationSpec = tween(durationMillis = 350),
            modifier = Modifier.fillMaxSize(),
            label = "HeroBackdropCrossfade"
        ) { banner ->
            AsyncImage(
                model = banner.backdropUrl.ifBlank { banner.posterUrl },
                contentDescription = banner.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Floating Card Carousel at the bottom with overhanging poster, title, meta & emerald play button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(start = 12.dp, end = 52.dp),
                pageSpacing = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val actualIndex = (page % banners.size).coerceIn(0, banners.size - 1)
                val banner = banners[actualIndex]

                // Card Item with overhanging poster on the left
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onBannerClick(banner)
                        }
                        .testTag("hero_banner_card_$actualIndex"),
                    contentAlignment = Alignment.BottomStart
                ) {
                    // Dark Charcoal Card Container
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFF222630),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 60.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Title & Subtitle Info Column
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = banner.title,
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (banner.isSeries) Icons.Default.Tv else Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color(0xFF9EA3AE),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "| ${banner.releaseYear.ifBlank { "2026" }} | ${banner.genre.split(",").firstOrNull()?.trim() ?: "Action"}",
                                        color = Color(0xFF9EA3AE),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Vibrant Red Play Button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE50914))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onPlayClick?.invoke(banner) ?: onBannerClick(banner)
                                    }
                                    .testTag("hero_play_button_$actualIndex"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Overhanging Vertical Poster on the Left
                    AsyncImage(
                        model = banner.posterUrl.ifBlank { banner.backdropUrl },
                        contentDescription = banner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(start = 8.dp, bottom = 3.dp)
                            .width(44.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                }
            }
        }
    }
}
