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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground
import com.hutube.app.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    catalog: CatalogData,
    onRefresh: () -> Unit,
    onPlayMedia: (MediaItem, MediaItem?) -> Unit,
    onOpenShow: (ShowItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val history = remember { mutableStateOf(HuTubeApplication.instance.watchHistoryManager.getHistory()) }
    var selectedNav by remember { mutableIntStateOf(0) }

    // Pick a featured item — prefer a show with a thumbnail
    val featuredItem = catalog.anime.firstOrNull { it is ShowItem && !(it as ShowItem).thumbnailLink.isNullOrEmpty() }
        ?: catalog.series.firstOrNull { !it.thumbnailLink.isNullOrEmpty() }
        ?: catalog.movies.firstOrNull { !it.thumbnailLink.isNullOrEmpty() }
        ?: catalog.anime.firstOrNull()
        ?: catalog.series.firstOrNull()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F0F0F),
                contentColor = Color.White,
                modifier = Modifier.height(64.dp),
                tonalElevation = 0.dp
            ) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandRed,
                    unselectedIconColor = Color(0xFF808080),
                    indicatorColor = Color.Transparent
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(26.dp)) },
                    label = { Text("Home", fontSize = 10.sp) },
                    selected = selectedNav == 0,
                    onClick = { selectedNav = 0 },
                    colors = colors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(26.dp)) },
                    label = { Text("Search", fontSize = 10.sp) },
                    selected = false,
                    onClick = onOpenSearch,
                    colors = colors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(26.dp)) },
                    label = { Text("Downloads", fontSize = 10.sp) },
                    selected = false,
                    onClick = onOpenDownloads,
                    colors = colors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(26.dp)) },
                    label = { Text("Settings", fontSize = 10.sp) },
                    selected = false,
                    onClick = onOpenSettings,
                    colors = colors
                )
            }
        },
        containerColor = Color(0xFF0b0b0b)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // === HERO BANNER ===
            item {
                HeroSection(
                    item = featuredItem,
                    isScanning = catalog.isScanning,
                    onRefresh = onRefresh,
                    onPlay = {
                        when (featuredItem) {
                            is ShowItem -> {
                                val firstEp = featuredItem.allEpisodes.firstOrNull()
                                val nextEp = featuredItem.allEpisodes.getOrNull(1)
                                if (firstEp != null) onPlayMedia(firstEp, nextEp)
                            }
                            is MediaItem -> onPlayMedia(featuredItem, null)
                        }
                    },
                    onInfo = {
                        if (featuredItem is ShowItem) onOpenShow(featuredItem)
                    }
                )
            }

            // === CONTINUE WATCHING ===
            if (history.value.isNotEmpty()) {
                item {
                    SectionHeader(title = "Continue Watching", icon = "▶")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history.value.take(10)) { item ->
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

            // === CATEGORY ROWS ===
            val categories = listOf(
                Triple(Category.ANIME, catalog.anime, "anime"),
                Triple(Category.CARTOON, catalog.cartoon, "cartoon"),
                Triple(Category.SERIES, catalog.series.map { it as Any }, "series"),
                Triple(Category.MOVIES, catalog.movies.map { it as Any }, "movies"),
                Triple(Category.NEWS, catalog.news.map { it as Any }, "news")
            )

            for ((category, items, _) in categories) {
                if (items.isNotEmpty()) {
                    item {
                        SectionHeader(title = category.title, icon = category.icon, count = items.size)
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                PosterCard(
                                    item = item,
                                    onClick = {
                                        when (item) {
                                            is ShowItem -> onOpenShow(item)
                                            is MediaItem -> onPlayMedia(item, null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ===== HERO SECTION =====
@Composable
private fun HeroSection(
    item: Any?,
    isScanning: Boolean,
    onRefresh: () -> Unit,
    onPlay: () -> Unit,
    onInfo: () -> Unit
) {
    val isShow = item is ShowItem
    val title = when (item) {
        is ShowItem -> item.title
        is MediaItem -> item.title
        else -> ""
    }
    val thumbUrl = when (item) {
        is ShowItem -> item.thumbnailLink
        is MediaItem -> item.thumbnailLink
        else -> null
    }
    val categoryName = when (item) {
        is ShowItem -> item.category.title
        is MediaItem -> item.category.title
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        // Background image
        if (!thumbUrl.isNullOrEmpty()) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1a1a1a)))
        }

        // Top scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0b0b0b), Color.Transparent))
                )
        )

        // Bottom scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF0b0b0b).copy(alpha = 0.85f), Color(0xFF0b0b0b))
                    )
                )
        )

        // Top bar — Logo + refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("u", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Tube", color = BrandRed, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }

            IconButton(onClick = onRefresh) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandRed, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                }
            }
        }

        // Hero content
        if (item != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Category badge
                Surface(
                    color = BrandRed.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = categoryName.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play button
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Info button (for shows only)
                    if (isShow) {
                        OutlinedButton(
                            onClick = onInfo,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                            )
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Info", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ===== SECTION HEADER =====
@Composable
private fun SectionHeader(title: String, icon: String = "", count: Int = 0) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon.isNotEmpty()) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count",
                color = Color(0xFF666666),
                fontSize = 12.sp
            )
        }
    }
}

// ===== POSTER CARD (Cloudstream-style vertical poster) =====
@Composable
private fun PosterCard(
    item: Any,
    onClick: () -> Unit
) {
    val isShow = item is ShowItem
    val title = if (isShow) (item as ShowItem).title else (item as MediaItem).title
    val thumbUrl = if (isShow) (item as ShowItem).thumbnailLink else (item as MediaItem).thumbnailLink
    val badge = if (!isShow) (item as MediaItem).resolution else null
    val episodeCount = if (isShow) {
        val count = (item as ShowItem).totalEpisodesCount
        if (count > 0) "$count eps" else null
    } else null

    // Watch progress for non-show items
    val progressPercent = if (!isShow) {
        val savedPos = HuTubeApplication.instance.watchHistoryManager.getProgress((item as MediaItem).id)
        val durationMs = item.durationMillis ?: 0L
        if (durationMs > 0 && savedPos > 0) (savedPos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    } else 0f

    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
    ) {
        // Poster image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1a1a1a))
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isShow) Icons.Default.Tv else Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color(0xFF444444),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom gradient
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))
                    )
            )

            // Quality badge
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(BrandRed, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = badge, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Episode count badge
            if (episodeCount != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(text = episodeCount, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Watch progress bar
            if (progressPercent > 0f) {
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
                            .fillMaxWidth(progressPercent)
                            .background(BrandRed)
                    )
                }
            }
        }

        // Title below poster
        Text(
            text = title,
            color = Color(0xFFCCCCCC),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
        )
    }
}

// ===== CONTINUE WATCHING CARD =====
@Composable
private fun ContinueWatchingCard(
    item: WatchProgress,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1a1a1a))
            .border(
                width = if (isFocused) 2.dp else 0.dp,
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
            }

            // Play icon overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            // Progress bar at bottom
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
                Text(
                    text = "$it · Ep ${item.episode ?: 1}",
                    color = Color(0xFF888888),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
