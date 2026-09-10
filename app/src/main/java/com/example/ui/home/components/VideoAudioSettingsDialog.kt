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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class PlayerAudioTrack(
    val id: String,
    val label: String,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1,
    val isDub: Boolean = false,
    val dubSubjectId: String = "",
    val dubDetailPath: String = ""
)

/**
 * Reusable Video and Audio Settings Dialog:
 * - Tabs for VIDEO and AUDIO
 * - Options list is smoothly scrollable with weight(1f, fill = false)
 * - Maximum height bounded so CANCEL and OK buttons are NEVER pushed off screen
 *   even in landscape fullscreen mode
 * - Custom circular radio selection indicators matching the app theme
 */
@Composable
fun VideoAudioSettingsDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    videoResolutionOptions: List<String>,
    currentVideoQuality: String,
    onVideoQualitySelected: (String) -> Unit,
    availableAudioTracks: List<PlayerAudioTrack>,
    currentAudioTrackId: String,
    onAudioTrackSelected: (PlayerAudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!show) return

    val filteredResolutions = remember(videoResolutionOptions) {
        val list = videoResolutionOptions.filter {
            !it.equals("None", ignoreCase = true) && !it.equals("Auto", ignoreCase = true)
        }
        if (list.isEmpty()) {
            listOf("3840 × 2160", "1920 × 1080", "1280 × 720")
        } else {
            list
        }
    }

    var activeTab by remember { mutableStateOf("VIDEO") }
    var tempSelectedQuality by remember(currentVideoQuality, filteredResolutions) {
        val initial = if (currentVideoQuality.equals("Auto", ignoreCase = true) || currentVideoQuality.equals("None", ignoreCase = true)) {
            filteredResolutions.firstOrNull() ?: "1920 × 1080"
        } else {
            val matching = filteredResolutions.firstOrNull { opt ->
                val optClean = opt.filter { it.isDigit() }
                val curClean = currentVideoQuality.filter { it.isDigit() }
                opt.equals(currentVideoQuality, ignoreCase = true) ||
                    (optClean.isNotBlank() && curClean.isNotBlank() && (optClean.contains(curClean) || curClean.contains(optClean)))
            }
            matching ?: filteredResolutions.firstOrNull() ?: currentVideoQuality
        }
        mutableStateOf(initial)
    }
    var tempSelectedAudioTrackId by remember(currentAudioTrackId) { mutableStateOf(currentAudioTrackId) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1B2430), // Dark slate navy
            modifier = modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(0.9f)
                .heightIn(max = 290.dp) // Constrained so OK and Cancel are ALWAYS visible in fullscreen
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp)
            ) {
                // 1. TABS: VIDEO | AUDIO (Fixed at top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // VIDEO TAB
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = "VIDEO" }
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "VIDEO",
                            color = if (activeTab == "VIDEO") Color(0xFF3B82F6) else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        if (activeTab == "VIDEO") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(2.5.dp)
                                    .background(Color(0xFF3B82F6), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    // AUDIO TAB
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = "AUDIO" }
                            .padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "AUDIO",
                            color = if (activeTab == "AUDIO") Color(0xFF3B82F6) else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        if (activeTab == "AUDIO") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(2.5.dp)
                                    .background(Color(0xFF3B82F6), RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 2. SCROLLABLE OPTIONS LIST (Takes flexible middle space, scrolls smoothly)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    if (activeTab == "VIDEO") {
                        filteredResolutions.forEach { opt ->
                            val optDigits = opt.filter { it.isDigit() }
                            val tempDigits = tempSelectedQuality.filter { it.isDigit() }
                            val isSelected = tempSelectedQuality.equals(opt, ignoreCase = true) ||
                                (optDigits.isNotBlank() && tempDigits.isNotBlank() && (optDigits.contains(tempDigits) || tempDigits.contains(optDigits)))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tempSelectedQuality = opt }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = opt,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(
                                            BorderStroke(
                                                2.dp,
                                                if (isSelected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.7f)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF3B82F6), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val tracks = if (availableAudioTracks.isEmpty()) {
                            listOf(PlayerAudioTrack("auto", "Default Audio"))
                        } else availableAudioTracks

                        tracks.forEach { audio ->
                            val isSelected = tempSelectedAudioTrackId == audio.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tempSelectedAudioTrackId = audio.id }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = audio.label,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (audio.isDub) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF3B82F6).copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "DUB",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(
                                            BorderStroke(
                                                2.dp,
                                                if (isSelected) Color(0xFF3B82F6) else Color.White.copy(alpha = 0.7f)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF3B82F6), CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 3. ACTION BUTTONS: CANCEL & OK (Always visible, pinned at bottom)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "CANCEL",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            if (tempSelectedQuality != currentVideoQuality) {
                                onVideoQualitySelected(tempSelectedQuality)
                            }
                            if (tempSelectedAudioTrackId != currentAudioTrackId) {
                                val selectedTrack = availableAudioTracks.firstOrNull { it.id == tempSelectedAudioTrackId }
                                    ?: PlayerAudioTrack(tempSelectedAudioTrackId, tempSelectedAudioTrackId)
                                onAudioTrackSelected(selectedTrack)
                            }
                            onDismissRequest()
                        }
                    ) {
                        Text(
                            text = "OK",
                            color = Color(0xFF3B82F6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
