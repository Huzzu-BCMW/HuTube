package com.hutube.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground
import com.hutube.app.ui.theme.DarkBackground

@Composable
fun SignInScreen(
    initialClientId: String = "",
    initialClientSecret: String = "",
    onStartGoogleAuth: (clientId: String, clientSecret: String) -> Unit,
    onManualTokenSubmit: (String) -> Unit
) {
    val context = LocalContext.current
    var clientId by remember { mutableStateOf(initialClientId) }
    var clientSecret by remember { mutableStateOf(initialClientSecret) }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var showManualToken by remember { mutableStateOf(false) }
    var manualToken by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

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
                    if (clientId.isBlank()) {
                        showCredentialsDialog = true
                    } else {
                        try {
                            onStartGoogleAuth(clientId.trim(), clientSecret.trim())
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
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
                    if (clientId.isNotBlank()) "OAuth Configured (Edit)" else "Configure Google OAuth Client",
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
                text = "HuTube connects directly via HTTP Range requests to bypass preview playback errors.",
                color = Color.DarkGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
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
                        text = "To let HuTube access your Google Drive without SHA-1 certification issues:\n\n" +
                                "1. Open Google Cloud Console (APIs & Services > Credentials)\n" +
                                "2. Create an OAuth 2.0 Client ID\n" +
                                "   • Type: Web Application\n" +
                                "   • Authorized Redirect URI: hutube://oauth2callback\n" +
                                "3. Paste your Client ID and Client Secret below:",
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
                            onStartGoogleAuth(clientId.trim(), clientSecret.trim())
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
