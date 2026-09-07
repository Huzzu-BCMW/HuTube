package com.hutube.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.hutube.app.data.drive.CatalogData
import com.hutube.app.data.drive.DriveRepository
import com.hutube.app.data.drive.GoogleDriveService
import com.hutube.app.data.drive.OAuthManager
import com.hutube.app.data.model.MediaItem
import com.hutube.app.data.model.ShowItem
import com.hutube.app.player.PlayerActivity
import com.hutube.app.ui.screens.HomeScreen
import com.hutube.app.ui.screens.SeriesDetailScreen
import com.hutube.app.ui.screens.SignInScreen
import com.hutube.app.ui.theme.HuTubeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var oauthManager: OAuthManager
    private lateinit var driveService: GoogleDriveService
    private lateinit var driveRepository: DriveRepository

    private var accessToken by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oauthManager = OAuthManager(this)
        driveService = GoogleDriveService {
            // Retrieve valid token (or refreshed token)
            oauthManager.getAccessToken()
        }
        driveRepository = DriveRepository(driveService)

        // Check if previously logged in
        if (oauthManager.hasToken()) {
            accessToken = oauthManager.getAccessToken()
            lifecycleScope.launch {
                // Ensure token is fresh
                val validToken = oauthManager.getValidToken()
                accessToken = validToken
                driveRepository.scanDrive()
            }
        }

        // Handle OAuth callback if launched via deep link
        handleAuthIntent(intent)

        setContent {
            HuTubeTheme {
                val catalog by driveRepository.catalog.collectAsState()
                var selectedShow by remember { mutableStateOf<ShowItem?>(null) }

                if (accessToken.isNullOrEmpty()) {
                    SignInScreen(
                        initialClientId = oauthManager.getClientId(),
                        initialClientSecret = oauthManager.getClientSecret(),
                        oauthManager = oauthManager,
                        onAuthSuccess = { token ->
                            accessToken = token
                            Toast.makeText(this@MainActivity, "Connected to Google Drive! 🍿", Toast.LENGTH_SHORT).show()
                            lifecycleScope.launch {
                                driveRepository.scanDrive()
                            }
                        },
                        onManualTokenSubmit = { token ->
                            oauthManager.saveAccessToken(token)
                            accessToken = token
                            lifecycleScope.launch {
                                driveRepository.scanDrive()
                            }
                        }
                    )
                } else if (selectedShow != null) {
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
                } else {
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
                            Toast.makeText(this, "Disconnected Google Drive", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthIntent(intent)
    }

    /**
     * Intercepts "hutube://oauth2callback?code=..." from Chrome Custom Tabs
     */
    private fun handleAuthIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "hutube" && uri.host == "oauth2callback") {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")

            if (error != null) {
                Toast.makeText(this, "Google Sign-In Cancelled: $error", Toast.LENGTH_LONG).show()
                return
            }

            if (code != null) {
                lifecycleScope.launch {
                    Toast.makeText(this@MainActivity, "Exchanging token...", Toast.LENGTH_SHORT).show()
                    val result = oauthManager.exchangeCodeForToken(code)
                    if (result.isSuccess) {
                        accessToken = result.getOrNull()
                        Toast.makeText(this@MainActivity, "Connected to Google Drive! 🍿", Toast.LENGTH_LONG).show()
                        driveRepository.scanDrive()
                    } else {
                        val errMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                        Toast.makeText(this@MainActivity, "Token error: $errMsg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
