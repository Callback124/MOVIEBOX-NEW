package com.example.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.download.DownloadItem
import com.example.data.download.DownloadStatus
import com.example.data.download.MovieDownloadManager
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MovieBoxRed

@Composable
fun DownloadScreen(
    onBackClick: () -> Unit,
    onPlayOffline: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val downloadManager = remember { MovieDownloadManager.getInstance(context) }
    val downloads by downloadManager.downloads.collectAsState()

    val downloadingList = remember(downloads) {
        downloads.filter { it.status != DownloadStatus.COMPLETED }
    }
    val completedList = remember(downloads) {
        downloads.filter { it.status == DownloadStatus.COMPLETED }
    }

    var selectedTabIndex by remember {
        mutableIntStateOf(if (downloadingList.isNotEmpty()) 0 else if (completedList.isNotEmpty()) 1 else 0)
    }

    BackHandler {
        onBackClick()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .testTag("download_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("downloads_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "Downloads",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            if (downloadingList.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MovieBoxRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MovieBoxRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(MovieBoxRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${downloadingList.size} Active",
                            color = MovieBoxRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // Tabs: "Downloading (X)" vs "Downloaded (Y)"
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkBackground,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MovieBoxRed,
                        height = 3.dp
                    )
                }
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF27272A))
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Downloading",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 0) Color.White else Color(0xFF94A3B8)
                        )
                        if (downloadingList.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (selectedTabIndex == 0) MovieBoxRed else Color(0xFF3F3F46),
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${downloadingList.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )

            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Downloaded",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == 1) Color.White else Color(0xFF94A3B8)
                        )
                        if (completedList.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (selectedTabIndex == 1) Color(0xFF10B981) else Color(0xFF3F3F46),
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${completedList.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedTabIndex == 0) {
                // Downloading List
                if (downloadingList.isEmpty()) {
                    EmptyDownloadState(
                        icon = Icons.Default.FileDownload,
                        title = "No active downloads",
                        subtitle = "When you download a movie while watching, it will appear here with live speed and progress."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(downloadingList, key = { it.id }) { item ->
                            DownloadingItemCard(
                                item = item,
                                onPause = { downloadManager.pauseDownload(item.id) },
                                onResume = { downloadManager.resumeDownload(item.id) },
                                onDelete = { downloadManager.deleteDownload(item.id) }
                            )
                        }
                    }
                }
            } else {
                // Completed List
                if (completedList.isEmpty()) {
                    EmptyDownloadState(
                        icon = Icons.Default.FileDownloadOff,
                        title = "No downloaded movies yet",
                        subtitle = "Downloaded movies are saved offline on your device so you can watch them anytime without internet."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(completedList, key = { it.id }) { item ->
                            DownloadedItemCard(
                                item = item,
                                onPlayOffline = { onPlayOffline(item) },
                                onDelete = { downloadManager.deleteDownload(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadingItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Poster
                AsyncImage(
                    model = item.coverUrl.ifBlank { item.backdropUrl },
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(width = 60.dp, height = 85.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Quality
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MovieBoxRed.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MovieBoxRed.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = item.quality,
                                color = MovieBoxRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // 2. Downloaded Language
                        if (item.dubLabel.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF3F3F46)
                            ) {
                                Text(
                                    text = item.dubLabel,
                                    color = Color(0xFFE4E4E7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 3. S01 E01 FORMAT
                        if (item.isSeries && (item.seasonNumber > 0 || item.episodeNumber > 0)) {
                            val sNum = item.seasonNumber.coerceAtLeast(1)
                            val eNum = item.episodeNumber.coerceAtLeast(1)
                            val seFormatted = "S${sNum.toString().padStart(2, '0')}  E${eNum.toString().padStart(2, '0')}"
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = seFormatted,
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status & Live Download Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (item.status) {
                                DownloadStatus.DOWNLOADING -> "Downloading..."
                                DownloadStatus.PAUSED -> "Paused"
                                DownloadStatus.FAILED -> "Failed"
                                else -> "Waiting..."
                            },
                            color = when (item.status) {
                                DownloadStatus.DOWNLOADING -> MovieBoxRed
                                DownloadStatus.PAUSED -> Color(0xFFFBBF24)
                                DownloadStatus.FAILED -> Color(0xFFEF4444)
                                else -> Color(0xFF94A3B8)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (item.status == DownloadStatus.DOWNLOADING && item.speedFormatted.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = item.speedFormatted,
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause / Resume & Delete actions
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (item.status == DownloadStatus.DOWNLOADING) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = MovieBoxRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Cancel download",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MovieBoxRed,
                trackColor = Color(0xFF27272A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Progress text: MB / MB & %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val downloadedStr = MovieDownloadManager.formatBytes(item.downloadedBytes)
                val totalStr = if (item.totalBytes > 0) MovieDownloadManager.formatBytes(item.totalBytes) else "..."
                Text(
                    text = "$downloadedStr / $totalStr",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Text(
                    text = "${(item.progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DownloadedItemCard(
    item: DownloadItem,
    onPlayOffline: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayOffline() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster with green Offline checkmark badge
            Box(
                modifier = Modifier
                    .size(width = 65.dp, height = 90.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.coverUrl.isNotBlank() || item.backdropUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.coverUrl.ifBlank { item.backdropUrl },
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Offline ready",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title Row with Delete Button on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete downloaded movie",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Full-width metadata row: Quality + Dub language + S01 E01
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Quality
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MovieBoxRed.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, MovieBoxRed.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = item.quality,
                            color = MovieBoxRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // 2. Downloaded Language
                    if (item.dubLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF27272A)
                        ) {
                            Text(
                                text = item.dubLabel,
                                color = Color(0xFFE4E4E7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 3. S01 E01 FORMAT
                    if (item.isSeries && (item.seasonNumber > 0 || item.episodeNumber > 0)) {
                        val sNum = item.seasonNumber.coerceAtLeast(1)
                        val eNum = item.episodeNumber.coerceAtLeast(1)
                        val seFormatted = "S${sNum.toString().padStart(2, '0')}  E${eNum.toString().padStart(2, '0')}"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = seFormatted,
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Size
                Text(
                    text = "Size: ${MovieDownloadManager.formatBytes(item.totalBytes.takeIf { it > 0 } ?: item.downloadedBytes)}",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = DarkSurfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
