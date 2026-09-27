package com.hutube.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.hutube.app.data.drive.DriveRepository
import com.hutube.app.data.drive.GoogleDriveService
import com.hutube.app.data.drive.OAuthManager
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.ShowItem
import com.hutube.app.player.PlayerActivity
import com.hutube.app.ui.screens.HomeScreen
import com.hutube.app.ui.screens.SeriesDetailScreen
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.DarkBackground
import com.hutube.app.ui.theme.HuTubeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var oauthManager: OAuthManager
    private lateinit var driveService: GoogleDriveService
    private lateinit var driveRepository: DriveRepository

    private var accessToken by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(true)
    private var errorMessage by mutableStateOf<String?>(null)
    private var debugInfo by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oauthManager = OAuthManager(this)

        driveService = GoogleDriveService {
            kotlinx.coroutines.runBlocking {
                try {
                    oauthManager.getValidToken()
                } catch (e: Exception) {
                    null
                }
            }
        }
        driveRepository = DriveRepository(driveService)

        val hasClientId = BuildConfig.DEFAULT_CLIENT_ID.isNotEmpty()
        val hasClientSecret = BuildConfig.DEFAULT_CLIENT_SECRET.isNotEmpty()
        val hasRefreshToken = BuildConfig.DEFAULT_REFRESH_TOKEN.isNotEmpty()
        debugInfo = "ClientID: ${if (hasClientId) "✅" else "❌"} | Secret: ${if (hasClientSecret) "✅" else "❌"} | RefreshToken: ${if (hasRefreshToken) "✅" else "❌"}"
        Log.d("MainActivity", "Credentials check: $debugInfo")

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting silent authentication...")

                val token = oauthManager.silentSignIn()

                if (token != null) {
                    accessToken = token
                    // Feed token to Coil so thumbnails load with auth
                    HuTubeApplication.instance.currentAccessToken = token
                    Log.d("MainActivity", "Silent auth successful, scanning Drive...")

                    driveRepository.scanDrive()

                    val catalog = driveRepository.catalog.value
                    val totalItems = catalog.anime.size + catalog.cartoon.size + catalog.series.size + catalog.movies.size + catalog.news.size

                    if (catalog.error != null) {
                        errorMessage = "Scan Failed:\n${catalog.error}"
                    } else if (totalItems == 0) {
                        errorMessage = "No matching folders found.\n\nMake sure you have folders named Anime, Cartoon, Series, Movies, or News in your Google Drive."
                    }

                    isLoading = false
                } else {
                    Log.e("MainActivity", "Silent auth failed")
                    errorMessage = "Authentication failed. Check your credentials."
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Auth exception: ${e.message}", e)
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }

        setContent {
            HuTubeTheme {
                val catalog by driveRepository.catalog.collectAsState()
                var selectedShow by remember { mutableStateOf<ShowItem?>(null) }
                var showDownloads by remember { mutableStateOf(false) }
                var showSearch by remember { mutableStateOf(false) }

                // Handle back button properly
                BackHandler(enabled = selectedShow != null || showDownloads || showSearch) {
                    when {
                        selectedShow != null -> selectedShow = null
                        showDownloads -> showDownloads = false
                        showSearch -> showSearch = false
                    }
                }

                when {
                    isLoading -> {
                        SplashScreen()
                    }

                    errorMessage != null -> {
                        ErrorScreen(message = errorMessage!!)
                    }

                    showDownloads -> {
                        val downloader = remember { com.hutube.app.data.download.DownloadManagerHelper(this@MainActivity) }
                        val files = downloader.getDownloadedFiles()

                        com.hutube.app.ui.screens.DownloadsScreen(
                            files = files,
                            onBack = { showDownloads = false },
                            onPlayFile = { media ->
                                PlayerActivity.start(
                                    context = this@MainActivity,
                                    media = media,
                                    nextMedia = null,
                                    token = accessToken ?: ""
                                )
                            },
                            onDelete = { file ->
                                file.delete()
                                // Force recompose by toggling
                                showDownloads = false
                                showDownloads = true
                            }
                        )
                    }

                    showSearch -> {
                        com.hutube.app.ui.screens.SearchScreen(
                            allItems = catalog.allVideos + catalog.series.flatMap { listOf(it as Any) } + catalog.anime + catalog.cartoon,
                            onBack = { showSearch = false },
                            onPlayMedia = { media ->
                                PlayerActivity.start(
                                    context = this@MainActivity,
                                    media = media,
                                    nextMedia = null,
                                    token = accessToken ?: ""
                                )
                            },
                            onOpenShow = { show ->
                                showSearch = false
                                selectedShow = show
                            }
                        )
                    }

                    selectedShow != null -> {
                        SeriesDetailScreen(
                            show = selectedShow!!,
                            onBack = { selectedShow = null },
                            onPlayEpisode = { media, nextMedia ->
                                PlayerActivity.start(
                                    context = this,
                                    media = media,
                                    nextMedia = nextMedia,
                                    token = accessToken ?: ""
                                )
                            },
                            onDownload = { media ->
                                val downloader = com.hutube.app.data.download.DownloadManagerHelper(this)
                                downloader.startDownload(media, accessToken ?: "")
                                Toast.makeText(this, "Downloading: ${media.title}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    else -> {
                        HomeScreen(
                            catalog = catalog,
                            onRefresh = {
                                lifecycleScope.launch {
                                    val validToken = oauthManager.getValidToken()
                                    accessToken = validToken
                                    HuTubeApplication.instance.currentAccessToken = validToken
                                    driveRepository.scanDrive()
                                }
                            },
                            onPlayMedia = { media, nextMedia ->
                                PlayerActivity.start(
                                    context = this,
                                    media = media,
                                    nextMedia = nextMedia,
                                    token = accessToken ?: ""
                                )
                            },
                            onOpenShow = { show ->
                                selectedShow = show
                            },
                            onOpenDownloads = { showDownloads = true },
                            onOpenSearch = { showSearch = true },
                            onOpenSettings = {
                                // Show app info toast — NOT a logout
                                Toast.makeText(
                                    this,
                                    "HuTube v${BuildConfig.VERSION_NAME}\nItems loaded: ${catalog.allVideos.size} videos",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean splash screen — just logo and spinner, no debug spam.
 */
@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text("Hu", color = Color.White, fontWeight = FontWeight.Black, fontSize = 48.sp)
                Text("Tube", color = BrandRed, fontWeight = FontWeight.Black, fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = BrandRed,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Loading your library...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Error screen with details.
 */
@Composable
private fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Row {
                Text("Hu", color = Color.White, fontWeight = FontWeight.Black, fontSize = 36.sp)
                Text("Tube", color = BrandRed, fontWeight = FontWeight.Black, fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "⚠️ Something went wrong",
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
