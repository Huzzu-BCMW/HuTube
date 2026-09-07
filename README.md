# 🍿 HuTube - Personal Google Drive OTT Streaming App

HuTube transforms your personal Google Drive into a high-performance **Netflix / YouTube style streaming app**, specifically engineered to eliminate Google Drive's annoying *"Unable to play this video at this time"* playback errors.

---

## ⚡ Why Google Drive Playback Fails & How HuTube Fixes It
* **Google Drive Web:** Converts files using Google's shared preview encoders. It fails on MKVs, unsupported audio tracks (AC3/E-AC-3/DTS), large high-bitrate files, or when your account hits the preview playback quota.
* **How RS File Manager / X-plore plays smoothly:** They bypass Google's preview transcoder and stream the raw file chunks directly using **HTTP `Range` requests**.
* **HuTube:** Runs a local high-speed Node.js streaming proxy that connects to the Google Drive API (`alt=media`), passing `Range: bytes=start-end` requests directly to your OTT video player. This provides **instant seeking, zero transcoding delays, and zero Google Drive preview errors**.

---

## 🎯 Features

* 🎌 **5 Pre-configured Drive Categories**:
  - **Anime** (Show & episode organization, season tabs)
  - **Cartoon** (Show & episode cards)
  - **Series** (Season selection, natural episode ordering)
  - **Movies** (Direct playback, resolution badges, duration)
  - **Funny Breaking News** (Shorts & clip grid with fast playback)
* 📺 **Drive Folders Browser**: Browse any other folder or subfolder across your entire Google Drive with breadcrumbs.
* 🍿 **Netflix / YouTube Video Player**:
  - Auto-hiding cinema overlay
  - **Skip Intro (+85s)** button (perfect for anime & TV show intros)
  - **Next Episode** button with 5s countdown auto-play
  - Volume slider, mute, 10s forward/backward seek
  - Playback speed control (0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x)
  - Keyboard shortcuts: `Space` (play/pause), `F` (fullscreen), `M` (mute), `Left/Right` (seek 10s), `Up/Down` (volume)
* 🚀 **FFmpeg Live Remuxing**:
  - Built-in fallback for audio tracks (like AC3/EAC3/DTS) unsupported by standard web browsers.
* 🎬 **External Player Support (VLC / PotPlayer)**:
  - 1-click **Download M3U Playlist** or copy stream URL to open directly in VLC, MPV, or PotPlayer.
* 🕒 **Continue Watching**:
  - Automatically saves your playback timestamps and progress percentage in localStorage so you can resume where you left off.

---

## 🚀 How to Launch HuTube

1. Simply double-click **`start-hutube.bat`** (or run `npm start` in the terminal).
2. It will open **`http://localhost:5000`** in your web browser.

---

## 🔑 2-Minute Google Drive Connection Setup

When you first open HuTube, click **"Connect Drive"**:

1. Open [Google Cloud Credentials Console](https://console.cloud.google.com/apis/credentials).
2. Create or select a project, then enable the **Google Drive API** in *APIs & Services > Library*.
3. Go to *APIs & Services > Credentials*, click **Create Credentials > OAuth client ID**:
   - Application Type: **Web application**
   - Name: **HuTube**
   - Authorized redirect URIs: `http://localhost:5000/auth/callback`
4. Copy your **Client ID** and **Client Secret**, paste them into HuTube's Setup window, and click **"Sign in with Google"**.
5. Grant read-only access to your Google Drive (`drive.readonly`).
6. HuTube will automatically scan your 5 folders (**Anime**, **Cartoon**, **Series**, **Movies**, **Funny Breaking News**) and your library will load on the home screen!
