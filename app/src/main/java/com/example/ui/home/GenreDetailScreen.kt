package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategorySection
import com.example.data.model.MovieItem
import com.example.ui.home.components.MovieCard
import com.example.ui.home.components.MovieLandscapeCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.MovieBoxRed

@Composable
fun GenreDetailScreen(
    section: CategorySection,
    movies: List<MovieItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isShortsPage: Boolean = false,
    isLandscape: Boolean = false,
    onBackClick: () -> Unit,
    onMovieClick: (MovieItem, List<MovieItem>, Boolean) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Android hardware / gesture navigation handles back navigation
    BackHandler(onBack = onBackClick)

    val gridState = rememberLazyGridState()

    // Continuously detect when user scrolls near the end to fetch more data
    val shouldLoadMore by remember(hasMore, isLoadingMore, isLoading) {
        derivedStateOf {
            if (!hasMore || isLoadingMore || isLoading) return@derivedStateOf false
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Trigger fetch when within last 8 items or scrolled to the end
            lastVisibleIndex >= totalItems - 8
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .testTag("genre_detail_screen_${section.id}")
    ) {
        // Top Header Bar: Clean header with ONLY the title (back icon, count, and extra text removed)
        Surface(
            color = DarkSurface.copy(alpha = 0.95f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Accent indicator bar
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MovieBoxRed)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Title only
                Text(
                    text = section.title,
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("genre_detail_title")
                )
            }
        }

        val effectiveIsShorts = isShortsPage || section.isShortsSection
        val effectiveIsLandscape = isLandscape || section.isLandscapeDetected

        // Body Content: Movies Grid
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && movies.isEmpty()) {
                // Initial Loading State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MovieBoxRed,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading titles...",
                        color = Color(0xFFA1A1AA),
                        fontSize = 13.sp
                    )
                }
            } else if (movies.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No movies found in this category",
                        color = Color(0xFFA1A1AA),
                        fontSize = 14.sp
                    )
                }
            } else {
                // Movie Grid with Infinite Scroll pagination
                val gridColumns = if (effectiveIsLandscape) {
                    GridCells.Adaptive(minSize = 150.dp)
                } else {
                    GridCells.Adaptive(minSize = 108.dp)
                }

                LazyVerticalGrid(
                    columns = gridColumns,
                    state = gridState,
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (effectiveIsLandscape) 12.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (effectiveIsLandscape) 14.dp else 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(movies, key = { index, item ->
                        if (item.id.isNotBlank()) "${item.id}_$index" else "${item.title}_$index"
                    }) { index, movie ->
                        // Fallback check on individual item composition near the end
                        if (index >= movies.size - 4 && hasMore && !isLoadingMore && !isLoading) {
                            LaunchedEffect(index) {
                                onLoadMore()
                            }
                        }

                        if (effectiveIsLandscape) {
                            MovieLandscapeCard(
                                movie = movie,
                                onClick = { onMovieClick(movie, movies, effectiveIsShorts) },
                                modifier = Modifier.fillMaxWidth(),
                                useFixedDimension = false,
                                forceShortsTag = effectiveIsShorts
                            )
                        } else {
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie, movies, effectiveIsShorts) },
                                modifier = Modifier.fillMaxWidth(),
                                useFixedDimension = false,
                                forceShortsTag = effectiveIsShorts
                            )
                        }
                    }

                    // Loading more footer indicator while data is being fetched
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MovieBoxRed,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Loading more movies...",
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 12.sp
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
