package com.hutube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
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
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.ShowItem
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground
import com.hutube.app.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    allItems: List<Any>,
    onBack: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onOpenShow: (ShowItem) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val results = remember(query, allItems) {
        if (query.length < 2) {
            emptyList()
        } else {
            val q = query.lowercase()
            allItems.filter { item ->
                when (item) {
                    is MediaItem -> item.title.lowercase().contains(q) ||
                            item.seriesTitle?.lowercase()?.contains(q) == true ||
                            item.name.lowercase().contains(q)
                    is ShowItem -> item.title.lowercase().contains(q)
                    else -> false
                }
            }.distinctBy {
                when (it) {
                    is MediaItem -> it.id
                    is ShowItem -> it.id
                    else -> it.hashCode()
                }
            }.take(50)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search movies, shows...", color = Color.Gray) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = BrandRed,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        if (query.length < 2) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Search your library", color = Color.Gray, fontSize = 16.sp)
                }
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No results for \"$query\"", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(results) { item ->
                    when (item) {
                        is MediaItem -> SearchResultRow(
                            title = item.title,
                            subtitle = buildString {
                                item.seriesTitle?.let { append("$it · ") }
                                item.formattedDuration.takeIf { it.isNotEmpty() }?.let { append(it) }
                                item.resolution?.let { append(" · $it") }
                            },
                            thumbnailUrl = item.thumbnailLink,
                            isShow = false,
                            onClick = { onPlayMedia(item) }
                        )
                        is ShowItem -> SearchResultRow(
                            title = item.title,
                            subtitle = "${item.totalEpisodesCount} episodes · ${item.category.title}",
                            thumbnailUrl = item.thumbnailLink,
                            isShow = true,
                            onClick = { onOpenShow(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isShow: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(80.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(CardBackground)
        ) {
            if (!thumbnailUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        if (isShow) Icons.Default.Tv else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isShow) {
            Surface(
                color = BrandRed,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "SERIES",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
