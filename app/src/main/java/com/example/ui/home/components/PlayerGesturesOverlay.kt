package com.example.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PlayerGestureType {
    NONE,
    SWIPE_SEEK,
    SWIPE_BRIGHTNESS,
    SWIPE_VOLUME
}

data class PlayerSeekHudState(
    val targetMs: Long,
    val durationMs: Long,
    val deltaMs: Long,
    val isForward: Boolean
)

data class PlayerDoubleTapFeedback(
    val isRight: Boolean, // true = +10 sec, false = -10 sec
    val timestamp: Long = System.currentTimeMillis()
)

fun formatGestureTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = (millis / 1000).toInt()
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Visual Overlay HUD for Video Player Gestures:
 * 1. Double tap to seek (+10 sec / -10 sec)
 * 2. Swipe to seek (Position scrub & delta)
 * 3. Swipe to change settings (Brightness & Volume indicators)
 */
@Composable
fun PlayerGesturesOverlay(
    doubleTapFeedback: PlayerDoubleTapFeedback?,
    seekState: PlayerSeekHudState?,
    isBrightnessHudVisible: Boolean,
    brightnessFraction: Float,
    isVolumeHudVisible: Boolean,
    volumeFraction: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Double Tap to Seek Feedback (+10 sec or -10 sec)
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.fillMaxSize()
        ) {
            doubleTapFeedback?.let { feedback ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (!feedback.isRight) {
                        // Left side: -10 sec
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 24.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                                .padding(horizontal = 22.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Rewind 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Text(
                                    text = "-10 sec",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Right side: +10 sec
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                                .padding(horizontal = 22.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "+10 sec",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 10 seconds",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Swipe to Seek HUD (Side to Side)
        AnimatedVisibility(
            visible = seekState != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            seekState?.let { state ->
                Box(
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                                contentDescription = if (state.isForward) "Seek Forward" else "Seek Backward",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                            val deltaSec = (state.deltaMs / 1000).toInt()
                            val deltaText = if (deltaSec >= 0) "+${deltaSec}s" else "${deltaSec}s"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (state.isForward) Color(0xFF2E7D32) else Color(0xFFC62828)
                            ) {
                                Text(
                                    text = deltaText,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "${formatGestureTime(state.targetMs)} / ${formatGestureTime(state.durationMs)}",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.durationMs > 0) {
                            val progressFraction = (state.targetMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .height(4.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Swipe to Change Settings: Brightness HUD (Left Side Slide)
        AnimatedVisibility(
            visible = isBrightnessHudVisible && seekState == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val pct = (brightnessFraction * 100).toInt().coerceIn(0, 100)
            val icon = when {
                pct >= 66 -> Icons.Default.BrightnessHigh
                pct >= 33 -> Icons.Default.BrightnessMedium
                else -> Icons.Default.BrightnessLow
            }

            Box(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Brightness",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Brightness: $pct%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(brightnessFraction.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(Color.White, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // 4. Swipe to Change Settings: Volume HUD (Right Side Slide)
        AnimatedVisibility(
            visible = isVolumeHudVisible && seekState == null && !isBrightnessHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val pct = (volumeFraction * 100).toInt().coerceIn(0, 100)
            val icon = if (pct <= 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp

            Box(
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Volume: $pct%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(volumeFraction.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }
    }
}
