package com.hutube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hutube.app.HuTubeApplication
import com.hutube.app.data.drive.CatalogData
import com.hutube.app.data.model.Category
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.ShowItem
import com.hutube.app.data.model.WatchProgress
import com.hutube.app.ui.components.CategoryRow
import com.hutube.app.ui.components.HeroBanner
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground
import com.hutube.app.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalog: CatalogData,
    onRefresh: () -> Unit,
    onPlayMedia: (MediaItem, MediaItem?) -> Unit, // media, nextMedia
    onOpenShow: (ShowItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val history = remember { mutableStateOf(HuTubeApplication.instance.watchHistoryManager.getHistory()) }

    // Pick featured item (Movie, Series, or Anime)
    val featuredItem = catalog.movies.firstOrNull()
        ?: catalog.series.firstOrNull()
        ?: catalog.anime.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandRed,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Tube", color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        if (catalog.isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandRed, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.LightGray)
                        }
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground.copy(alpha = 0.95f),
                    scrolledContainerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Featured Banner
            if (featuredItem != null) {
                item {
                    HeroBanner(
                        item = featuredItem,
                        onPlay = {
                            if (featuredItem is ShowItem) {
                                val firstEp = featuredItem.allEpisodes.firstOrNull()
                                val nextEp = featuredItem.allEpisodes.getOrNull(1)
                                if (firstEp != null) onPlayMedia(firstEp, nextEp)
                            } else if (featuredItem is MediaItem) {
                                onPlayMedia(featuredItem, null)
                            }
                        },
                        onDetails = {
                            if (featuredItem is ShowItem) onOpenShow(featuredItem)
                        }
                    )
                }
            }

            // Continue Watching Shelf
            if (history.value.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(
                            text = "🕒  Continue Watching",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(history.value) { item ->
                                ContinueWatchingCard(
                                    item = item,
                                    onClick = {
                                        val media = MediaItem(
                                            id = item.fileId,
                                            name = item.title,
                                            title = item.title,
                                            seriesTitle = item.seriesTitle,
                                            season = item.season,
                                            episode = item.episode,
                                            thumbnailLink = item.thumbnailLink,
                                            durationMillis = item.durationMs
                                        )
                                        onPlayMedia(media, null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5 Category Rows
            item {
                CategoryRow(
                    category = Category.ANIME,
                    items = catalog.anime,
                    onItemClick = { item ->
                        if (item is ShowItem) onOpenShow(item)
                        else if (item is MediaItem) onPlayMedia(item, null)
                    }
                )
            }

            item {
                CategoryRow(
                    category = Category.CARTOON,
                    items = catalog.cartoon,
                    onItemClick = { item ->
                        if (item is ShowItem) onOpenShow(item)
                        else if (item is MediaItem) onPlayMedia(item, null)
                    }
                )
            }

            item {
                CategoryRow(
                    category = Category.SERIES,
                    items = catalog.series,
                    onItemClick = { item ->
                        if (item is ShowItem) onOpenShow(item)
                    }
                )
            }

            item {
                CategoryRow(
                    category = Category.MOVIES,
                    items = catalog.movies,
                    onItemClick = { item ->
                        if (item is MediaItem) onPlayMedia(item, null)
                    }
                )
            }

            item {
                CategoryRow(
                    category = Category.NEWS,
                    items = catalog.news,
                    onItemClick = { item ->
                        if (item is MediaItem) onPlayMedia(item, null)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: WatchProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBackground)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (!item.thumbnailLink.isNullOrEmpty()) {
                AsyncImage(
                    model = item.thumbnailLink,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF222222)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray)
                }
            }

            // Progress Bar
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
                        .fillMaxWidth(item.progressPercent)
                        .background(BrandRed)
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.seriesTitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$it · Ep ${item.episode ?: 1}",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
