package com.hutube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hutube.app.data.model.Category
import com.hutube.app.data.model.MediaItem
import com.hutube.app.ui.theme.DarkBackground
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    files: List<File>,
    onBack: () -> Unit,
    onPlayFile: (MediaItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads", color = Color.White) },
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
        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No downloaded files found.", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(files) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val item = MediaItem(
                                    id = "file://${file.absolutePath}",
                                    name = file.name,
                                    title = file.nameWithoutExtension,
                                    seriesTitle = null,
                                    category = Category.MOVIES,
                                    season = 1,
                                    episode = null,
                                    thumbnailLink = null,
                                    durationMillis = null,
                                    resolution = null,
                                    size = file.length(),
                                    mimeType = "video/mp4"
                                )
                                onPlayFile(item)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(file.nameWithoutExtension, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("${file.length() / (1024 * 1024)} MB", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
