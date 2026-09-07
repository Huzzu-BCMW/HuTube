package com.hutube.app.data.drive

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class OAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("hutube_auth", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().build()

    companion object {
        const val REDIRECT_URI = "hutube://oauth2callback"
        const val SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

        // Default or user-provided credentials
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_CLIENT_SECRET = "client_secret"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }

    fun saveCredentials(clientId: String, clientSecret: String) {
        prefs.edit()
            .putString(KEY_CLIENT_ID, clientId.trim())
            .putString(KEY_CLIENT_SECRET, clientSecret.trim())
            .apply()
    }

    fun getClientId(): String = prefs.getString(KEY_CLIENT_ID, "") ?: ""
    fun getClientSecret(): String = prefs.getString(KEY_CLIENT_SECRET, "") ?: ""

    fun saveAccessToken(token: String, refreshToken: String? = null, expiresInSeconds: Long = 3600) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token.trim())
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (expiresInSeconds * 1000))
        if (!refreshToken.isNullOrEmpty()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken.trim())
        }
        editor.apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun hasToken(): Boolean = !getAccessToken().isNullOrEmpty()

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    /**
     * Constructs the Google OAuth URL and opens it in Chrome Custom Tabs
     */
    fun startOAuthLogin(activityContext: Context, clientId: String? = null, clientSecret: String? = null) {
        val cId = if (!clientId.isNullOrEmpty()) {
            saveCredentials(clientId, clientSecret ?: "")
            clientId
        } else {
            getClientId()
        }

        if (cId.isEmpty()) {
            throw IllegalArgumentException("Client ID is required for Google Sign-In.")
        }

        val authUrl = StringBuilder(AUTH_ENDPOINT)
            .append("?client_id=").append(URLEncoder.encode(cId, "UTF-8"))
            .append("&redirect_uri=").append(URLEncoder.encode(REDIRECT_URI, "UTF-8"))
            .append("&response_type=code")
            .append("&scope=").append(URLEncoder.encode(SCOPE, "UTF-8"))
            .append("&access_type=offline")
            .append("&prompt=consent")
            .toString()

        val uri = Uri.parse(authUrl)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(activityContext, uri)
        } catch (e: Exception) {
            // Fallback to standard browser intent
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            activityContext.startActivity(browserIntent)
        }
    }

    /**
     * Exchanges auth code for access_token and refresh_token
     */
    suspend fun exchangeCodeForToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cId = getClientId()
            val cSecret = getClientSecret()

            val formBuilder = FormBody.Builder()
                .add("code", code)
                .add("client_id", cId)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")

            if (cSecret.isNotEmpty()) {
                formBuilder.add("client_secret", cSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Token exchange failed ($response.code): $body"))
                }

                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)
                val expiresIn = json.optLong("expires_in", 3600L)

                saveAccessToken(accessToken, refreshToken, expiresIn)
                Result.success(accessToken)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refreshes the access token using the stored refresh_token if expired
     */
    suspend fun getValidToken(): String? = withContext(Dispatchers.IO) {
        val currentToken = getAccessToken() ?: return@withContext null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val refreshToken = getRefreshToken()

        // If expires in more than 2 minutes, token is still valid
        if (System.currentTimeMillis() < (expiresAt - 120000) || refreshToken.isNullOrEmpty()) {
            return@withContext currentToken
        }

        // Refresh the token
        try {
            val formBuilder = FormBody.Builder()
                .add("refresh_token", refreshToken)
                .add("client_id", getClientId())
                .add("grant_type", "refresh_token")

            val cSecret = getClientSecret()
            if (cSecret.isNotEmpty()) {
                formBuilder.add("client_secret", cSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext currentToken
                val body = response.body?.string() ?: return@withContext currentToken
                val json = JSONObject(body)
                val newToken = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)

                saveAccessToken(newToken, null, expiresIn)
                return@withContext newToken
            }
        } catch (e: Exception) {
            return@withContext currentToken
        }
    }
}
