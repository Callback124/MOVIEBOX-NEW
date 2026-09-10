package com.example.ui.home.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MovieItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MovieBoxRed

data class PresetStream(
    val title: String,
    val format: String,
    val url: String
)

val PRESET_STREAMS = listOf(
    PresetStream(
        title = "ForBiggerBlazes.mp4",
        format = "MP4",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
    ),
    PresetStream(
        title = "Live HLS Stream (Mux)",
        format = "HLS .m3u8",
        url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    ),
    PresetStream(
        title = "Akamai Multi-Bitrate DASH",
        format = "DASH .mpd",
        url = "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd"
    ),
    PresetStream(
        title = "Big Buck Bunny 1080p",
        format = "MP4",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    ),
    PresetStream(
        title = "Tears of Steel",
        format = "MKV/MP4",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )
)

@Composable
fun NetworkStreamDialog(
    onDismiss: () -> Unit,
    onPlayStream: (MovieItem) -> Unit
) {
    var streamUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4") }
    var streamTitle by remember { mutableStateOf("ForBiggerBlazes.mp4") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MovieBoxRed,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stream,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Network Stream",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA1A1AA)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Plays any network stream or video link with universal codec support (HLS .m3u8, DASH .mpd, SmoothStreaming .ism, RTSP, MP4, MKV, WebM, TS, FLV).",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Stream URL Input
                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = {
                        streamUrl = it
                        isError = false
                        // Auto derive title from filename
                        if (streamTitle == "Network Stream" || streamTitle == "ForBiggerBlazes.mp4" || streamTitle.isBlank()) {
                            val candidate = it.substringAfterLast("/").substringBefore("?")
                            if (candidate.isNotBlank() && candidate.length > 3) {
                                streamTitle = candidate
                            }
                        }
                    },
                    label = { Text("Stream / Video URL") },
                    placeholder = { Text("https://... or rtsp://...") },
                    singleLine = true,
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MovieBoxRed,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )

                // Optional Title Input
                OutlinedTextField(
                    value = streamTitle,
                    onValueChange = { streamTitle = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text("ForBiggerBlazes.mp4") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MovieBoxRed,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Presets Title
                Text(
                    text = "Quick Presets & Samples:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Presets Horizontal list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PRESET_STREAMS) { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (streamUrl == preset.url) MovieBoxRed.copy(alpha = 0.25f) else DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (streamUrl == preset.url) MovieBoxRed else DarkSurfaceBorder
                            ),
                            modifier = Modifier.clickable {
                                streamUrl = preset.url
                                streamTitle = preset.title
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = preset.format,
                                    color = MovieBoxRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (streamUrl.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val finalTitle = streamTitle.ifBlank {
                        streamUrl.substringAfterLast("/").substringBefore("?").ifBlank { "Network Stream" }
                    }
                    val streamMovie = MovieItem(
                        id = "stream_${System.currentTimeMillis()}",
                        title = finalTitle,
                        directUrl = streamUrl.trim()
                    )
                    onPlayStream(streamMovie)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MovieBoxRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("play_stream_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Play Stream", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA1A1AA))
            }
        }
    )
}
