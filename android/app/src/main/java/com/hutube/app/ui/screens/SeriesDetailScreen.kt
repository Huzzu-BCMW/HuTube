package com.hutube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.hutube.app.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    show: ShowItem,
    onBack: () -> Unit,
    onPlayEpisode: (MediaItem, MediaItem?) -> Unit
) {
    val hasSeasons = show.seasons.isNotEmpty()
    var selectedSeasonIdx by remember { mutableIntStateOf(0) }

    val currentEpisodes = if (hasSeasons) {
        show.seasons.getOrNull(selectedSeasonIdx)?.episodes ?: emptyList()
    } else {
        show.episodes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(show.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Info
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Poster
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
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

                    // Metadata
                    Column(modifier = Modifier.weight(1f)) {
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

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = show.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (hasSeasons) "${show.seasons.size} Seasons" else "${show.totalEpisodesCount} Episodes",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val first = currentEpisodes.firstOrNull()
                                val next = currentEpisodes.getOrNull(1)
                                if (first != null) onPlayEpisode(first, next)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Season Tabs (if multiple seasons)
            if (hasSeasons && show.seasons.size > 1) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(show.seasons) { idx, season ->
                            FilterChip(
                                selected = selectedSeasonIdx == idx,
                                onClick = { selectedSeasonIdx = idx },
                                label = { Text(season.name, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = CardBackground,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            // Episodes List
            itemsIndexed(currentEpisodes) { idx, episode ->
                val nextEp = currentEpisodes.getOrNull(idx + 1)
                val savedPos = HuTubeApplication.instance.watchHistoryManager.getProgress(episode.id)
                val duration = episode.durationMillis ?: 0L
                val progress = if (duration > 0 && savedPos > 0) (savedPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayEpisode(episode, nextEp) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Index Number
                    Text(
                        text = "${episode.episode ?: (idx + 1)}",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(24.dp)
                    )

                    // Episode Thumbnail (16:9)
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                    ) {
                        if (!episode.thumbnailLink.isNullOrEmpty()) {
                            AsyncImage(
                                model = episode.thumbnailLink,
                                contentDescription = episode.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Play overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        // Progress bar
                        if (progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.DarkGray)
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
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (episode.formattedDuration.isNotEmpty()) {
                                Text(episode.formattedDuration, color = Color.Gray, fontSize = 11.sp)
                            }
                            episode.resolution?.let {
                                Text(it, color = BrandRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
