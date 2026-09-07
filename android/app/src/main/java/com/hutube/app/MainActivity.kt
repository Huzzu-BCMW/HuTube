package com.hutube.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.hutube.app.data.drive.CatalogData
import com.hutube.app.data.drive.DriveRepository
import com.hutube.app.data.drive.GoogleDriveService
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

    private var googleSignInClient: GoogleSignInClient? = null
    private var accessToken by mutableStateOf<String?>(null)
    private lateinit var driveService: GoogleDriveService
    private lateinit var driveRepository: DriveRepository

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                fetchDriveAccessToken(account)
            } catch (e: Exception) {
                Toast.makeText(this, "Sign-in failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        driveService = GoogleDriveService { accessToken }
        driveRepository = DriveRepository(driveService)

        // Check if token already saved in SharedPreferences
        val prefs = getSharedPreferences("hutube_auth", Context.MODE_PRIVATE)
        val savedToken = prefs.getString("access_token", null)
        if (!savedToken.isNullOrEmpty()) {
            accessToken = savedToken
            lifecycleScope.launch {
                driveRepository.scanDrive()
            }
        }

        setupGoogleSignIn()

        setContent {
            HuTubeTheme {
                val catalog by driveRepository.catalog.collectAsState()
                var selectedShow by remember { mutableStateOf<ShowItem?>(null) }

                if (accessToken.isNullOrEmpty()) {
                    SignInScreen(
                        onGoogleSignIn = { startGoogleSignIn() },
                        onManualTokenSubmit = { token ->
                            accessToken = token
                            prefs.edit().putString("access_token", token).apply()
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
                            lifecycleScope.launch { driveRepository.scanDrive() }
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
                            // Search or filter can be triggered here
                        },
                        onOpenSettings = {
                            // Sign out
                            accessToken = null
                            prefs.edit().remove("access_token").apply()
                        }
                    )
                }
            }
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun startGoogleSignIn() {
        val client = googleSignInClient ?: return
        val signInIntent = client.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun fetchDriveAccessToken(account: GoogleSignInAccount) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val scope = "oauth2:${DriveScopes.DRIVE_READONLY}"
                val token = GoogleAuthUtil.getToken(
                    this@MainActivity,
                    account.account!!,
                    scope
                )

                withContext(Dispatchers.Main) {
                    accessToken = token
                    getSharedPreferences("hutube_auth", Context.MODE_PRIVATE)
                        .edit().putString("access_token", token).apply()
                    Toast.makeText(this@MainActivity, "Connected to Google Drive!", Toast.LENGTH_SHORT).show()
                }

                driveRepository.scanDrive()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Auth error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
