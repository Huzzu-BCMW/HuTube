package com.hutube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hutube.app.HuTubeApplication
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.ShowItem
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground

@Composable
fun MediaCard(
    item: Any, // Can be MediaItem or ShowItem
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isShow = item is ShowItem
    val title = if (isShow) (item as ShowItem).title else (item as MediaItem).title
    val thumbUrl = if (isShow) (item as ShowItem).thumbnailLink else (item as MediaItem).thumbnailLink
    val subtitle = if (isShow) {
        val sCount = (item as ShowItem).seasons.size
        if (sCount > 0) "$sCount Seasons" else "${(item as ShowItem).totalEpisodesCount} Episodes"
    } else {
        (item as MediaItem).formattedDuration
    }
    val badge = if (!isShow) (item as MediaItem).resolution else null

    val savedPos = if (!isShow) HuTubeApplication.instance.watchHistoryManager.getProgress((item as MediaItem).id) else 0L
    val durationMs = if (!isShow) (item as MediaItem).durationMillis ?: 0L else 0L
    val progressPercent = if (durationMs > 0 && savedPos > 0) {
        (savedPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .clickable { onClick() }
    ) {
        // Thumbnail Box (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (!thumbUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2B2B2B), Color(0xFF141414))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isShow) Icons.Default.Tv else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Quality Badge (e.g. 1080p, 4K)
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Watch Progress Bar
            if (progressPercent > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.DarkGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent)
                            .background(BrandRed)
                    )
                }
            }
        }

        // Title and Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
