package com.hutube.app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oauthManager = OAuthManager(this)
        driveService = GoogleDriveService {
            // Always provide a fresh token for every API call
            oauthManager.getAccessToken()
        }
        driveRepository = DriveRepository(driveService)

        // Silently authenticate on startup — ZERO login UI
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting silent authentication...")
                val token = oauthManager.silentSignIn()
                if (token != null) {
                    accessToken = token
                    Log.d("MainActivity", "Silent auth successful, scanning Drive...")
                    driveRepository.scanDrive()
                    isLoading = false
                } else {
                    Log.e("MainActivity", "Silent auth failed — no token returned")
                    errorMessage = "Authentication failed. Make sure GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET, and GDRIVE_REFRESH_TOKEN are set in GitHub Secrets."
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

                when {
                    // Loading splash while authenticating silently
                    isLoading -> {
                        SplashScreen()
                    }

                    // Error state — credentials are missing or broken
                    errorMessage != null -> {
                        ErrorScreen(message = errorMessage!!)
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
                            }
                        )
                    }

                    // Main home screen — the default landing page
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
                            onOpenSearch = {
                                // Search functionality
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
 * Splash screen shown while silently authenticating with Google Drive.
 * User sees this for ~1 second on first launch, then goes straight to content.
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
        }
    }
}

/**
 * Error screen shown only if credentials are missing from the build.
 * Normal users should never see this — it means the APK was built without secrets.
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text("Hu", color = Color.White, fontWeight = FontWeight.Black, fontSize = 36.sp)
                Text("Tube", color = BrandRed, fontWeight = FontWeight.Black, fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "⚠️ Configuration Error",
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
