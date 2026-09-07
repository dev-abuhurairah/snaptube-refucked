<div align="center">

# ⚡ VidSnap

### Fast, Clean & Modern Snaptube-Inspired Video & Audio Downloader for Android

[![Android CI](https://github.com/dev-abuhurairah/snaptube-refucked/actions/workflows/android.yml/badge.svg)](https://github.com/dev-abuhurairah/snaptube-refucked/actions/workflows/android.yml)
[![Latest Release](https://img.shields.io/github/v/release/dev-abuhurairah/snaptube-refucked?color=FFC83B&label=Release&style=flat-square)](https://github.com/dev-abuhurairah/snaptube-refucked/releases)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg?style=flat-square)](https://developer.android.com)
[![Ad-Free](https://img.shields.io/badge/Ads-Free-success.svg?style=flat-square)](#)

<p align="center">
  <b>Built with ❤️ by <a href="https://github.com/dev-abuhurairah">dev-abuhurairah</a></b>
</p>

---

[Features](#-key-features) •
[Download](#-download--install) •
[Flavors Explained](#-build-flavors) •
[Direct Share Guide](#-how-to-use) •
[Building](#-%EF%B8%8F-building-from-source) •
[License](#-license)

</div>

---

## 🌟 Overview

**VidSnap** is an open-source, ad-free Android media downloader engineered for speed, elegance, and simplicity. Combining the intuitive user experience of **Snaptube** with the unmatched scraping power of **yt-dlp**, **FFmpeg**, and **Aria2c**, VidSnap allows you to download video and audio from over 1,000+ websites in maximum quality with zero interruptions.

---

## 💡 Key Features

### 🎨 Snaptube-Inspired Modern UI
* **Clean Home Dashboard:** Centered golden *VidSnap* typography with a pill search bar and instant clipboard detection.
* **Compact Floating Bottom Sheet:** A modern floating card with side margins (`16dp`) and rounded corners (`24dp`) that never stretches across the entire screen.
* **Curated Format Cards:** Cleanly organized into **Music** (MP3 Classic, M4A Audio) and **Video** (4K, 2K, 1080p HD, 720p HD, 480p, 360p) with live file size estimates.
* **Single Radio Selection:** Visual check indicators for one-tap format picking, defaulting automatically to 720p HD / 1080p HD.
* **Expandable Extra Formats:** An accessible *"More formats (All >)"* toggle to explore alternative codecs and resolutions when needed.

### ⚡ Seamless Direct Share & Paste
* **1-Click Share Menu:** Share video links directly from **YouTube, Instagram, Facebook, TikTok, Twitter/X, WhatsApp, Reddit**, or web browsers to VidSnap. The download sheet pops up instantly.
* **Paste from Clipboard:** A prominent `[ 📋 Paste Link from Clipboard ]` button automatically detects copied links and triggers animated shimmer format extraction.

### 🚀 High-Speed Engine (yt-dlp + Aria2c + FFmpeg)
* **Multi-Connection Turbo Downloading:** Powered by Aria2c for parallel segment downloads that saturate your connection.
* **Lossless Audio/Video Muxing:** High-resolution video streams (1080p, 2K, 4K) are seamlessly combined with top-tier audio tracks using FFmpeg on device.
* **1000+ Supported Sites:** Supports YouTube, Reels, TikTok (no watermark), Twitter/X videos, SoundCloud, Vimeo, and [hundreds more](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md).

### 🎛️ Power-User Controls
* **Background Downloads:** Reliable background queuing via Android Jetpack `WorkManager`.
* **Video Chapter Cutting:** Trim or cut videos based on timestamps or embedded chapters.
* **SponsorBlock Integration:** Automatically skip sponsored segments, intros, and outros.
* **Metadata & Subtitles:** Embed thumbnails, chapter markers, and multilingual subtitles (`.srt` / `.vtt`).
* **Incognito Mode:** Download without saving logs or history.
* **Custom File Templates:** Organize downloads by `%(uploader)s/ %(title)s.%(ext)s` or custom naming templates.

---

## 📲 Download & Install

Download the latest APK directly from the [Releases](https://github.com/dev-abuhurairah/snaptube-refucked/releases) or the automated [GitHub Actions CI Artifacts](https://github.com/dev-abuhurairah/snaptube-refucked/actions):

| Package Flavor | Recommended For | Size | Auto-Updater |
|---|---|---|:---:|
| **[VidSnap (GitHub Release)](https://github.com/dev-abuhurairah/snaptube-refucked/releases)** | **Most Users (Direct APK)** | ~65 MB | ✅ Yes |
| **[VidSnap (FOSS Release)](https://github.com/dev-abuhurairah/snaptube-refucked/releases)** | F-Droid Store / Strict FOSS | ~65 MB | ❌ No |
| **[VidSnap (Izzy Release)](https://github.com/dev-abuhurairah/snaptube-refucked/releases)** | Lightweight / IzzyOnDroid | ~47 MB | ❌ No |

---

## 🔍 Build Flavors Explained

* **`github` (Recommended):** Fully bundled with Python 3.14, Aria2c, and FFmpeg. Includes built-in in-app update checking and installation so you always stay up to date.
* **`foss`:** Compliant with F-Droid inclusion guidelines. Self-updating code and package install permissions are excluded in accordance with F-Droid policies.
* **`izzy`:** Optimized for the IzzyOnDroid repository, using an external Python runtime layer to keep the initial APK footprint under 50 MB.

---

## 📖 How to Use

### Method 1: Share Link from Any App
1. Open any app (YouTube, Instagram, TikTok, Facebook, Twitter, browser, etc.).
2. Tap **Share** on any video.
3. Select **VidSnap** from the share sheet.
4. Pick your desired quality (Music MP3/M4A or Video 1080p/720p/etc.) and tap **Download**!

### Method 2: Copy & Paste
1. Copy the video link to your clipboard.
2. Open **VidSnap**.
3. Tap **"Paste Link from Clipboard"** or paste in the search bar.
4. Select your format and tap **Download**.

---

## 🛠️ Building from Source

### Prerequisites
* **Android Studio** (Hedgehog or newer recommended)
* **JDK 17** (Amazon Corretto or Eclipse Temurin)
* **Android SDK** (API 36, Build-Tools 36.0.0)

### Steps
```bash
# 1. Clone the repository
git clone https://github.com/dev-abuhurairah/snaptube-refucked.git
cd snaptube-refucked

# 2. Build the GitHub release APK
./gradlew assembleGithubRelease

# 3. Output APK location:
# app/build/outputs/apk/github/release/VidSnap-*.apk
```

---

## 🤝 Contributing

Contributions, bug reports, and suggestions are warmly welcome!
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/cool-feature`).
3. Commit your changes (`git commit -m 'feat: add cool feature'`).
4. Push to the branch (`git push origin feature/cool-feature`).
5. Open a Pull Request.

---

## 📜 License & Credits

* **Author:** [dev-abuhurairah](https://github.com/dev-abuhurairah) (`contactabuhurairah@gmail.com`)
* **License:** Distributed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for details.
* **Underlying Engines:**
  * [yt-dlp](https://github.com/yt-dlp/yt-dlp)
  * [ffmpeg-android-maker](https://github.com/Javernaut/ffmpeg-android-maker)
  * [aria2](https://github.com/aria2/aria2)
  * Foundation inspired by open-source YTDLnis project by Denis Çerri.

---

<div align="center">
  <b>VidSnap</b> — Fast & Simple Media Downloader for Android.<br>
  Developed with ❤️ by <a href="https://github.com/dev-abuhurairah">dev-abuhurairah</a>
</div>
