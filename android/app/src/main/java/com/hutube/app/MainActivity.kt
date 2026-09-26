package com.hutube.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
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

        // The token provider calls getValidToken() to always get a fresh token
        // (refreshes automatically if expired)
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

        // Check what credentials we have baked in
        val hasClientId = BuildConfig.DEFAULT_CLIENT_ID.isNotEmpty()
        val hasClientSecret = BuildConfig.DEFAULT_CLIENT_SECRET.isNotEmpty()
        val hasRefreshToken = BuildConfig.DEFAULT_REFRESH_TOKEN.isNotEmpty()
        debugInfo = "ClientID: ${if (hasClientId) "âœ…" else "âŒ"} | Secret: ${if (hasClientSecret) "âœ…" else "âŒ"} | RefreshToken: ${if (hasRefreshToken) "âœ…" else "âŒ"}"
        Log.d("MainActivity", "Credentials check: $debugInfo")

        // Silently authenticate on startup â€” ZERO login UI
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting silent authentication...")
                debugInfo += "\nâ³ Refreshing token..."

                val token = oauthManager.silentSignIn()

                if (token != null) {
                    accessToken = token
                    debugInfo += "\nâœ… Token: ${token.take(15)}..."
                    Log.d("MainActivity", "Silent auth successful, scanning Drive...")

                    debugInfo += "\nâ³ Scanning Google Drive..."
                    driveRepository.scanDrive()

                    val catalog = driveRepository.catalog.value
                    val totalItems = catalog.anime.size + catalog.cartoon.size + catalog.series.size + catalog.movies.size + catalog.news.size
                    debugInfo += "\nðŸ“Š anime=${catalog.anime.size} cartoon=${catalog.cartoon.size} series=${catalog.series.size} movies=${catalog.movies.size} news=${catalog.news.size}"

                    if (catalog.error != null) {
                        debugInfo += "\nâŒ Scan error: ${catalog.error}"
                        errorMessage = "Scan Failed:\n$debugInfo"
                    } else if (totalItems == 0) {
                        debugInfo += "\nâš ï¸ 0 items found. Check: Is Google Drive API enabled? Do you have folders named Anime/Cartoon/Series/Movies/News?"
                        errorMessage = "No matching folders found.\n\n$debugInfo"
                    }

                    isLoading = false
                } else {
                    Log.e("MainActivity", "Silent auth failed â€” no token returned")
                    debugInfo += "\nâŒ Token refresh returned null!"
                    errorMessage = "Authentication failed.\n\n$debugInfo"
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Auth exception: ${e.message}", e)
                debugInfo += "\nâŒ Exception: ${e.message}"
                errorMessage = "Error: ${e.message}\n\n$debugInfo"
                isLoading = false
            }
        }

        setContent {
            HuTubeTheme {
                val catalog by driveRepository.catalog.collectAsState()
                var selectedShow by remember { mutableStateOf<ShowItem?>(null) }
                var showDownloads by remember { mutableStateOf(false) }

                when {
                    // Loading splash while authenticating silently
                    isLoading -> {
                        SplashScreen(debugInfo)
                    }

                    // Error state â€” credentials are missing or broken
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
                            }
                        )
                    }

                    // Series detail view
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
                            }
                        )
                    }

                    // Main home screen â€” the default landing page
                    else -> {
                        HomeScreen(
                            catalog = catalog,
                            onRefresh = {
                                lifecycleScope.launch {
                                    val validToken = oauthManager.getValidToken()
                                    accessToken = validToken
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
                            onOpenSearch = {
                                // Show debug info as toast for diagnostics
                                Toast.makeText(this, debugInfo, Toast.LENGTH_LONG).show()
                            },
                            onOpenSettings = {
                                // Sign Out
                                oauthManager.clear()
                                accessToken = null
                                isLoading = false
                                errorMessage = "Signed out. Re-install APK with fresh credentials to reconnect."
                                Toast.makeText(this, "Disconnected Google Drive", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Splash screen with debug info visible so user can screenshot if it gets stuck.
 */
@Composable
private fun SplashScreen(debugInfo: String = "") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text("Hu", color = Color.White, fontWeight = FontWeight.Black, fontSize = 42.sp)
                Text("Tube", color = BrandRed, fontWeight = FontWeight.Black, fontSize = 42.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = BrandRed,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connecting to Google Drive...",
                color = Color.Gray,
                fontSize = 14.sp
            )

            if (debugInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = debugInfo,
                    color = Color.DarkGray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

/**
 * Error screen with full debug details so user can screenshot for diagnosis.
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
                text = "âš ï¸ Configuration Error",
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




