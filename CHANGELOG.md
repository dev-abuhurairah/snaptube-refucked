# ⚡ VidSnap Changelog

All notable changes to the **VidSnap** project will be documented in this file.

---

## [v1.8.9.2] - 2026-09-07

### 🎨 Snaptube-Inspired UI Overhaul
- **Modern Dashboard:** Replaced legacy layout with clean serif *VidSnap* typography and an integrated pill search bar.
- **Compact Floating Download Menu:** Designed a floating bottom sheet with `16dp` side margins, `24dp` rounded corners, and maximum width constraints.
- **Curated Formats:** Formats categorized cleanly into **Music** (MP3 Classic, M4A Audio) and **Video** (4K, 2K, 1080p HD, 720p HD, 480p, 360p) with file size indicators.
- **Single Radio Selection:** Visual check indicators for one-tap format picking, defaulting automatically to 720p HD / 1080p HD.
- **Floating Pill Bottom Navigation:** Floating bottom navigation bar with 20dp horizontal margins and 16dp bottom spacing.

### 🚀 New App Icon & Branding
- Updated vector and adaptive launcher icons to match Snaptube's iconic aesthetic:
  - Outer thick white circular ring.
  - Golden yellow circle (`#FFC83B`).
  - White inner TV screen display.
  - Dual speed bars and downward triangular arrowhead in vibrant orange (`#FF4500`).

### 🐛 Bug Fixes
- **Direct Video Share Crash Fixed:** Removed illegal `TYPE_APPLICATION_OVERLAY` call in `ShareActivity` that threw `BadTokenException` / `SecurityException` on Android 8.0+.
- **Transparent BottomSheet Theme:** Converted `ShareActivity` to standard transparent BottomSheet dialog theme.
- **URL Extraction:** Enhanced extraction across `EXTRA_TEXT`, `clipData`, `getCharSequenceExtra`, and `dataString`.
- **Auto Metadata Fetching:** Ensured `initUpdateData()` is triggered automatically when a video is shared or pasted, eliminating empty bottom sheets.

---

## Foundation
- Built upon open-source yt-dlp, FFmpeg, and Aria2c engines for multi-connection acceleration across 1,000+ websites.
