package com.example.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlatformItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MovieBoxRed

@Composable
fun PlatformRow(
    platforms: List<PlatformItem>,
    selectedPlatform: String?,
    onPlatformSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (platforms.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Streaming Hubs",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (selectedPlatform != null) {
                Text(
                    text = "Clear",
                    color = MovieBoxRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onPlatformSelected(null) }
                        .padding(4.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(platforms, key = { it.name }) { platform ->
                val isSelected = selectedPlatform == platform.name
                PlatformPill(
                    platform = platform,
                    isSelected = isSelected,
                    onClick = { onPlatformSelected(platform.name) }
                )
            }
        }
    }
}

@Composable
fun PlatformPill(
    platform: PlatformItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val brandColors = getPlatformColors(platform.name)

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) brandColors.first else Color(0xFF282736),
        label = "pill_border"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) brandColors.first.copy(alpha = 0.2f) else DarkSurface,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("platform_pill_${platform.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            // Platform Icon Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(brandColors.first)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = platform.name,
                color = if (isSelected) Color.White else Color(0xFFE4E4E7),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun getPlatformColors(name: String): Pair<Color, Color> {
    return when (name.lowercase()) {
        "netflix" -> Color(0xFFE50914) to Color(0xFFB81D24)
        "primevideo" -> Color(0xFF00A8E1) to Color(0xFF007399)
        "disney" -> Color(0xFF113CCF) to Color(0xFF0B2580)
        "appletv" -> Color(0xFFA2AAAD) to Color(0xFF7D8285)
        "hulu" -> Color(0xFF1CE783) to Color(0xFF14B866)
        "viu" -> Color(0xFFFFCC00) to Color(0xFFCC9900)
        "zee5" -> Color(0xFF8230C6) to Color(0xFF5D1F91)
        "vivamax" -> Color(0xFFFF5722) to Color(0xFFD84315)
        "hoichoi" -> Color(0xFFFF1744) to Color(0xFFC51162)
        "showmax" -> Color(0xFF00E5FF) to Color(0xFF00B0FF)
        else -> Color(0xFF6366F1) to Color(0xFF4F46E5)
    }
}
