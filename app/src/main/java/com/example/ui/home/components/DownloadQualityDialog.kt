package com.example.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.download.MovieDownloadManager
import com.example.data.model.MovieStream
import com.example.ui.theme.MovieBoxRed

data class DownloadQualityOption(
    val qualityLabel: String,
    val resolution: String,
    val description: String,
    val estimatedSize: String,
    val stream: MovieStream?
)

/**
 * Quality Selection Dialog for downloading the currently playing dub.
 * Displays available qualities with radio selection, dub info, and CANCEL / OK buttons.
 */
@Composable
fun DownloadQualityDialog(
    show: Boolean,
    movieTitle: String,
    currentDubLabel: String,
    availableStreams: List<MovieStream>,
    onDismissRequest: () -> Unit,
    onDownloadConfirmed: (quality: String, downloadUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    // Build quality options based on available streams of current dub
    val qualityOptions = remember(availableStreams) {
        val list = mutableListOf<DownloadQualityOption>()
        if (availableStreams.isNotEmpty()) {
            availableStreams.forEach { stream ->
                val digits = stream.resolution.split(",").firstOrNull()?.filter { it.isDigit() }?.ifBlank { "720" } ?: "720"
                val (label, desc, defaultEst) = when {
                    digits.contains("2160") || digits.contains("4k") -> Triple("2160p", "4K Ultra HD (Highest Quality)", "1.8 GB")
                    digits.contains("1440") || digits.contains("2k") -> Triple("1440p", "2K Quad HD", "1.1 GB")
                    digits.contains("1080") -> Triple("1080p", "Full HD (High Quality)", "650 MB")
                    digits.contains("720") -> Triple("720p", "HD (Standard Recommended)", "380 MB")
                    digits.contains("480") -> Triple("480p", "SD (Data Saver)", "210 MB")
                    digits.contains("360") -> Triple("360p", "Low (Fast Download)", "120 MB")
                    else -> Triple("${digits}p", "Standard", "300 MB")
                }

                val sizeText = if (stream.size > 0) {
                    MovieDownloadManager.formatBytes(stream.size)
                } else {
                    "~$defaultEst"
                }

                // Avoid duplicates in label
                if (list.none { it.qualityLabel == label }) {
                    list.add(
                        DownloadQualityOption(
                            qualityLabel = label,
                            resolution = stream.resolution,
                            description = desc,
                            estimatedSize = sizeText,
                            stream = stream
                        )
                    )
                }
            }
        }

        // Fallback default options if no streams yet loaded
        if (list.isEmpty()) {
            list.add(DownloadQualityOption("2160p", "2160", "4K Ultra HD (Highest Quality)", "~1.8 GB", null))
            list.add(DownloadQualityOption("1080p", "1080", "Full HD (High Quality)", "~650 MB", null))
            list.add(DownloadQualityOption("720p", "720", "HD (Standard Recommended)", "~380 MB", null))
            list.add(DownloadQualityOption("480p", "480", "SD (Data Saver)", "~210 MB", null))
        }
        list.sortedByDescending { opt ->
            opt.qualityLabel.filter { it.isDigit() }.toIntOrNull() ?: 0
        }
    }

    var selectedOption by remember(qualityOptions) {
        mutableStateOf(
            qualityOptions.first()
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1B2430), // Dark slate navy matching video settings
            border = BorderStroke(1.dp, Color(0xFF2D3748)),
            modifier = modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 420.dp)
                .padding(horizontal = 8.dp)
                .testTag("download_quality_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp, bottom = 8.dp, start = 20.dp, end = 20.dp)
            ) {
                // Header: Download Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MovieBoxRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MovieBoxRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Download Quality",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentDubLabel.isNotBlank()) {
                            Text(
                                text = "Current Audio: $currentDubLabel",
                                color = MovieBoxRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Quality Options List with custom circular Radio buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    qualityOptions.forEach { option ->
                        val isSelected = (selectedOption.qualityLabel == option.qualityLabel)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedOption = option }
                                .background(
                                    if (isSelected) Color(0xFF243042) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom circular radio indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) MovieBoxRed else Color(0xFF94A3B8),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(MovieBoxRed, CircleShape)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = option.qualityLabel,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option.estimatedSize,
                                        color = if (isSelected) MovieBoxRed else Color(0xFF94A3B8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = option.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons: CANCEL & OK
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("download_dialog_cancel")
                    ) {
                        Text(
                            text = "CANCEL",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            val streamUrl = selectedOption.stream?.url
                                ?: availableStreams.firstOrNull()?.url
                                ?: ""
                            onDownloadConfirmed(selectedOption.qualityLabel, streamUrl)
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("download_dialog_ok")
                    ) {
                        Text(
                            text = "OK",
                            color = MovieBoxRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
