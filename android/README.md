# 📱 HuTube - Native Android OTT App (Kotlin + Media3 ExoPlayer)

HuTube for Android is a native OTT streaming app written in **Kotlin** and **Jetpack Compose**, powered by **Media3 ExoPlayer**.

---

## ⚡ How HuTube Solves Google Drive Playback on Android

* **The Problem:** Google Drive's preview player forces transcoding on Google's web servers, which crashes on high-bitrate media, MKVs, multi-channel audio tracks, or quota limits.
* **The X-plore / RS File Manager Solution:** Android file managers like X-plore and RS File Manager stream Google Drive using **Android's ExoPlayer** with **direct HTTP Range requests**.
* **HuTube's Engine:** Implements `DriveMediaSourceFactory.kt` using ExoPlayer's `DefaultHttpDataSource`:
  ```kotlin
  val streamUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
  val httpDataSourceFactory = DefaultHttpDataSource.Factory()
      .setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
  ```
  ExoPlayer streams the raw chunks with status `206 Partial Content`, using Android's hardware `MediaCodec` to decode any video format with instantaneous seeking and 0 preview errors!

---

## 📂 Features

1. 🎌 **Automatic 5-Folder Organization**:
   * **Anime**: Shows, seasons, episode numbering, and progress bars.
   * **Cartoon**: Animated series and cartoon movies.
   * **Series**: Season selector tabs with episode lists.
   * **Movies**: High-resolution movie cards with duration and quality badges.
   * **Funny Breaking News**: Fast playback clip grid.
2. 🍿 **Full-Screen OTT Video Player (`PlayerActivity.kt`)**:
   * **Skip Intro (+85s)** button.
   * **Next Episode** queue with auto-play on episode end.
   * **10s Rewind / Fast-Forward** buttons.
   * **Aspect Ratio Switcher** (Fit, Fill, Zoom).
   * **Picture-in-Picture (PiP)** mode: continue watching while using other apps.
   * **Continue Watching**: Saves progress in `SharedPreferences` so you never lose your place.
3. 🔑 **Google Sign-In**:
   * Official Google Play Services Sign-In with `DriveScopes.DRIVE_READONLY`.
   * Option to paste an OAuth access token directly for quick testing.

---

## 🚀 How to Open and Build the APK in Android Studio

1. Download and install **[Android Studio](https://developer.android.com/studio)** if you haven't already.
2. Open Android Studio, click **File > Open**, and select the **`android`** folder inside `HuTube` (`c:\Users\coolh\OneDrive\Desktop\HuTube\android`).
3. Android Studio will automatically sync the Gradle dependencies.
4. **To Run Directly on your Phone / Android TV**:
   - Enable **Developer Options** and **USB Debugging** on your Android phone.
   - Plug your phone into your PC via USB (or connect via Wi-Fi debugging).
   - Click the green **Run (▶)** button in Android Studio.
5. **To Generate an Installable APK file**:
   - In Android Studio menu, click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   - Once finished, click **"locate"** to find your `app-debug.apk`.
   - Transfer `app-debug.apk` to your phone and tap to install!
