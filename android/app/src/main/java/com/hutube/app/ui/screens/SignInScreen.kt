package com.hutube.app.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hutube.app.data.drive.OAuthManager
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground
import com.hutube.app.ui.theme.DarkBackground

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SignInScreen(
    initialClientId: String = "",
    initialClientSecret: String = "",
    oauthManager: OAuthManager,
    onAuthSuccess: (token: String) -> Unit,
    onManualTokenSubmit: (String) -> Unit
) {
    var clientId by remember { mutableStateOf(initialClientId) }
    var clientSecret by remember { mutableStateOf(initialClientSecret) }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var showAuthWebView by remember { mutableStateOf(false) }
    var showManualToken by remember { mutableStateOf(false) }
    var manualToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isExchangingToken by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Determine current effective Client ID
    val effectiveClientId = if (clientId.isNotBlank()) clientId.trim() else oauthManager.getClientId()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // App Icon
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BrandRed,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text("Hu", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
                Text("Tube", color = BrandRed, fontWeight = FontWeight.Black, fontSize = 32.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Personal Google Drive OTT Streaming",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (errorMessage != null) {
                Surface(
                    color = Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Primary Google Sign-In Button
            Button(
                onClick = {
                    if (effectiveClientId.isBlank()) {
                        showCredentialsDialog = true
                    } else {
                        showAuthWebView = true
                    }
                },
                enabled = !isExchangingToken,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isExchangingToken) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Connecting...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Credentials Config Button
            OutlinedButton(
                onClick = { showCredentialsDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (effectiveClientId.isNotBlank()) "OAuth Configured (Edit)" else "Configure Google OAuth Client",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Or Enter Token Directly Toggle
            TextButton(onClick = { showManualToken = !showManualToken }) {
                Text(
                    text = if (showManualToken) "Hide Token Option" else "Or Paste Access Token Directly",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            if (showManualToken) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualToken,
                    onValueChange = { manualToken = it },
                    placeholder = { Text("ya29.a0AfH6...", color = Color.DarkGray, fontSize = 12.sp) },
                    label = { Text("Google Drive Access Token", color = Color.LightGray, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrandRed,
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (manualToken.isNotBlank()) {
                            onManualTokenSubmit(manualToken.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect with Token", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "HuTube streams directly via HTTP Range requests to bypass Google Drive playback limits.",
                color = Color.DarkGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    // In-App OAuth WebView Dialog
    if (showAuthWebView && effectiveClientId.isNotBlank()) {
        val redirectUri = OAuthManager.REDIRECT_URI
        val authUrl = oauthManager.getAuthUrl(effectiveClientId, redirectUri)

        Dialog(
            onDismissRequest = { showAuthWebView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sign in to Google Drive",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showAuthWebView = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    // Embedded WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.setSupportZoom(true)
                                // Set modern Chrome desktop/mobile User Agent so Google does not block WebView
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: ""

                                        // Intercept redirect with authorization code
                                        if (url.startsWith("http://localhost") || url.startsWith("https://localhost") || url.startsWith("hutube://")) {
                                            val uri = Uri.parse(url)
                                            val code = uri.getQueryParameter("code")
                                            val error = uri.getQueryParameter("error")

                                            if (error != null) {
                                                errorMessage = "Sign-in error: $error"
                                                showAuthWebView = false
                                                return true
                                            }

                                            if (code != null) {
                                                showAuthWebView = false
                                                isExchangingToken = true

                                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                    val result = oauthManager.exchangeCodeForToken(code, redirectUri)
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        isExchangingToken = false
                                                        if (result.isSuccess) {
                                                            onAuthSuccess(result.getOrNull() ?: "")
                                                        } else {
                                                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Token exchange failed"
                                                        }
                                                    }
                                                }
                                                return true
                                            }
                                        }
                                        return false
                                    }
                                }

                                loadUrl(authUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Google Cloud Credentials Dialog
    if (showCredentialsDialog) {
        AlertDialog(
            onDismissRequest = { showCredentialsDialog = false },
            title = {
                Text("Google Cloud OAuth Setup", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "In your Google Cloud Console (APIs & Services > Credentials):\n\n" +
                                "1. Click Create Credentials > OAuth client ID\n" +
                                "2. Choose: Web application\n" +
                                "3. In Authorized redirect URIs, enter:\n" +
                                "   http://localhost:5000/auth/callback\n" +
                                "4. Paste your Client ID & Client Secret below:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        label = { Text("Client ID", color = Color.LightGray, fontSize = 11.sp) },
                        placeholder = { Text("xxxx.apps.googleusercontent.com", color = Color.DarkGray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = clientSecret,
                        onValueChange = { clientSecret = it },
                        label = { Text("Client Secret", color = Color.LightGray, fontSize = 11.sp) },
                        placeholder = { Text("GOCSPX-xxxx", color = Color.DarkGray, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandRed,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCredentialsDialog = false
                        if (clientId.isNotBlank()) {
                            oauthManager.saveCredentials(clientId.trim(), clientSecret.trim())
                            showAuthWebView = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("Save & Sign In", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCredentialsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardBackground
        )
    }
}
