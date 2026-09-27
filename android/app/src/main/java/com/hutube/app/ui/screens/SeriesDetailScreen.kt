package com.hutube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    show: ShowItem,
    onBack: () -> Unit,
    onPlayEpisode: (MediaItem, MediaItem?) -> Unit,
    onDownload: (MediaItem) -> Unit
) {
    val hasSeasons = show.seasons.isNotEmpty()
    var selectedSeasonIdx by remember { mutableIntStateOf(0) }

    val currentEpisodes = if (hasSeasons) {
        show.seasons.getOrNull(selectedSeasonIdx)?.episodes ?: emptyList()
    } else {
        show.episodes
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0b0b0b))
    ) {
        // === BACKDROP HERO ===
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                // Backdrop image
                if (!show.thumbnailLink.isNullOrEmpty()) {
                    AsyncImage(
                        model = show.thumbnailLink,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1a1a1a)))
                }

                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF0b0b0b).copy(alpha = 0.7f), Color.Transparent))
                        )
                )

                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0b0b0b)))
                        )
                )

                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // === SHOW INFO ===
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-40).dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Poster
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161616))
                ) {
                    if (!show.thumbnailLink.isNullOrEmpty()) {
                        AsyncImage(
                            model = show.thumbnailLink,
                            contentDescription = show.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Details
                Column(modifier = Modifier.weight(1f).padding(top = 40.dp)) {
                    // Category badge
                    Surface(
                        color = BrandRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = show.category.title.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = show.title,
                        color = Color(0xFFE9EAEE),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Metadata
                    Text(
                        text = buildString {
                            if (hasSeasons) append("${show.seasons.size} Seasons")
                            else append("${show.totalEpisodesCount} Episodes")
                            append(" · ${show.totalEpisodesCount} Total Episodes")
                        },
                        color = Color(0xFF9BA0A4),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Play button
                    Button(
                        onClick = {
                            val first = currentEpisodes.firstOrNull()
                            val next = currentEpisodes.getOrNull(1)
                            if (first != null) onPlayEpisode(first, next)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // === SEASON TABS ===
        if (hasSeasons && show.seasons.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(show.seasons) { idx, season ->
                        FilterChip(
                            selected = selectedSeasonIdx == idx,
                            onClick = { selectedSeasonIdx = idx },
                            label = {
                                Text(
                                    season.name,
                                    fontWeight = if (selectedSeasonIdx == idx) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandRed,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2B2C30),
                                labelColor = Color(0xFF9BA0A4)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = null
                        )
                    }
                }
            }
        }

        // === EPISODES HEADER ===
        item {
            Text(
                text = "${currentEpisodes.size} Episodes",
                color = Color(0xFFE9EAEE),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // === EPISODE LIST ===
        itemsIndexed(currentEpisodes) { idx, episode ->
            val nextEp = currentEpisodes.getOrNull(idx + 1)
            val savedPos = HuTubeApplication.instance.watchHistoryManager.getProgress(episode.id)
            val duration = episode.durationMillis ?: 0L
            val progress = if (duration > 0 && savedPos > 0) (savedPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

            EpisodeRow(
                episode = episode,
                index = idx,
                progress = progress,
                onClick = { onPlayEpisode(episode, nextEp) },
                onDownload = { onDownload(episode) }
            )

            if (idx < currentEpisodes.size - 1) {
                HorizontalDivider(
                    color = Color(0xFF1a1a1a),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun EpisodeRow(
    episode: MediaItem,
    index: Int,
    progress: Float,
    onClick: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Episode number
        Text(
            text = "${episode.episode ?: (index + 1)}",
            color = Color(0xFF666666),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )

        // Thumbnail (16:9)
        Box(
            modifier = Modifier
                .width(126.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF161616))
        ) {
            if (!episode.thumbnailLink.isNullOrEmpty()) {
                AsyncImage(
                    model = episode.thumbnailLink,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Play icon overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Progress bar
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF333333))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(BrandRed)
                    )
                }
            }
        }

        // Title and metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = Color(0xFFE9EAEE),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (episode.formattedDuration.isNotEmpty()) {
                    Text(episode.formattedDuration, color = Color(0xFF9BA0A4), fontSize = 11.sp)
                }
                episode.resolution?.let {
                    Text(it, color = BrandRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Download button
        IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF9BA0A4), modifier = Modifier.size(20.dp))
        }
    }
}
