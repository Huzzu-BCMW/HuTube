package com.hutube.app.data.drive

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Zero-login OAuth Manager for HuTube.
 *
 * Credentials (Client ID, Client Secret, Refresh Token) are baked into BuildConfig
 * at build time via GitHub Secrets. On app launch, this manager silently exchanges
 * the refresh token for an access token — no UI, no WebView, no login screen.
 */
class OAuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("hutube_auth", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "OAuthManager"
        const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    // --- Credential Getters (BuildConfig first, SharedPreferences fallback) ---

    fun getClientId(): String {
        return com.hutube.app.BuildConfig.DEFAULT_CLIENT_ID.ifEmpty {
            prefs.getString("client_id", "") ?: ""
        }
    }

    fun getClientSecret(): String {
        return com.hutube.app.BuildConfig.DEFAULT_CLIENT_SECRET.ifEmpty {
            prefs.getString("client_secret", "") ?: ""
        }
    }

    /**
     * Returns the refresh token — baked-in from BuildConfig takes priority,
     * then falls back to any previously stored one in SharedPreferences.
     */
    fun getRefreshToken(): String {
        val bakedIn = com.hutube.app.BuildConfig.DEFAULT_REFRESH_TOKEN
        if (bakedIn.isNotEmpty()) return bakedIn
        return prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
    }

    /**
     * Returns true if we have all the credentials needed to silently authenticate.
     * This means: Client ID + Client Secret + Refresh Token are all non-empty.
     */
    fun hasCredentials(): Boolean {
        return getClientId().isNotEmpty() &&
               getClientSecret().isNotEmpty() &&
               getRefreshToken().isNotEmpty()
    }

    /**
     * Returns true if we already have a cached access token (may be expired).
     */
    fun hasToken(): Boolean = !getAccessToken().isNullOrEmpty()

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    private fun saveAccessToken(token: String, expiresInSeconds: Long = 3600) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token.trim())
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + (expiresInSeconds * 1000))
            .apply()
        Log.d(TAG, "Access token saved, expires in ${expiresInSeconds}s")
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    /**
     * THE CORE METHOD — called on app startup and before every API call.
     *
     * 1. If we have a valid (non-expired) cached access token → return it immediately.
     * 2. If the token is expired or missing → use the refresh token to get a new one silently.
     * 3. No UI involved whatsoever.
     *
     * Returns null only if credentials are missing or the refresh fails.
     */
    suspend fun getValidToken(): String? = withContext(Dispatchers.IO) {
        // Check if cached token is still valid (with 2-minute buffer)
        val cachedToken = getAccessToken()
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (!cachedToken.isNullOrEmpty() && System.currentTimeMillis() < (expiresAt - 120_000)) {
            Log.d(TAG, "Using cached access token (valid for ${(expiresAt - System.currentTimeMillis()) / 1000}s)")
            return@withContext cachedToken
        }

        // Token is expired or missing — refresh it silently
        val refreshToken = getRefreshToken()
        val clientId = getClientId()
        val clientSecret = getClientSecret()

        if (refreshToken.isEmpty() || clientId.isEmpty()) {
            Log.e(TAG, "Cannot refresh: missing credentials (clientId=${clientId.isNotEmpty()}, refreshToken=${refreshToken.isNotEmpty()})")
            return@withContext cachedToken // Return stale token as last resort
        }

        Log.d(TAG, "Refreshing access token silently...")

        try {
            val formBuilder = FormBody.Builder()
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .add("grant_type", "refresh_token")

            if (clientSecret.isNotEmpty()) {
                formBuilder.add("client_secret", clientSecret)
            }

            val request = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Token refresh failed (${response.code}): $body")
                    return@withContext cachedToken // Return stale token as fallback
                }

                val json = JSONObject(body)
                val newToken = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)

                saveAccessToken(newToken, expiresIn)
                Log.d(TAG, "Token refreshed successfully!")
                return@withContext newToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh exception: ${e.message}", e)
            return@withContext cachedToken
        }
    }

    /**
     * Performs the initial silent authentication on first app launch.
     * Uses the baked-in refresh token to obtain the very first access token.
     * Returns the access token on success, null on failure.
     */
    suspend fun silentSignIn(): String? {
        if (!hasCredentials()) {
            Log.e(TAG, "Silent sign-in impossible: missing credentials")
            return null
        }
        return getValidToken()
    }
}
