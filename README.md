<div align="center">

<img src="smarttubetv/src/stmobile/res/drawable/ic_comet_logo.png" width="128" height="128" alt="CometTube Logo">

# CometTube

### Next-Generation YouTube Client with iOS-Style Liquid Glass UI & Advanced Playback

[![Release: 1.0](https://img.shields.io/badge/Release-v1.0-3894FF?style=for-the-badge&logo=android&logoColor=white)](https://github.com/techbymarcon/comettube/releases)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026--35%2B)-00C853?style=for-the-badge&logo=android&logoColor=white)](#download--installation)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)
[![Theme: Pure OLED Black](https://img.shields.io/badge/OLED-Pure%20Black%20%23000000-000000?style=for-the-badge&logo=safari&logoColor=white)](#design--theming)

<br>

**CometTube** is a high-performance, privacy-respecting YouTube client for Android phones and tablets, engineered with a custom **iOS Liquid Glass** floating navigation bar, native **YouTube Shorts** integration, pure OLED black theming, and smooth fluid micro-interactions.

Built on the rock-solid media engine of **SmartTube** and touch foundation of **NewTube**, CometTube delivers an uncompromising mobile experience without Google Play Services or microG.

<br>

[**Download APK**](#download--installation) • [**Key Features**](#key-features) • [**Liquid Glass Optics**](#liquid-glass-optical-physics) • [**Setup Flow**](#first-time-setup-experience) • [**Building from Source**](#building-from-source) • [**Credits**](#credits--acknowledgments)

</div>

---

## Key Features

### 💎 iOS Liquid Glass Floating Dock
* **Real Backdrop Sampling**: Live contents scroll underneath the floating capsule dock; the background is recorded into hardware `RenderNode` / `GraphicsLayer` buffers with zero viewport clipping.
* **Dual-Pass AGSL Shader Pipeline**: Multi-pass directional Gaussian blur (`TileMode.MIRROR`) chained with Android Graphics Shading Language (AGSL API 33+) RuntimeShader computing inverse-power SDF edge refractions, chromatic dispersion, directional specular rim highlights, and vibrancy boosting.
* **Interactive Ball Hold & Glide Physics**: Hold the active indicator ball to expand it into an enlarged 3D liquid glass droplet (`1.38x` expansion with haptic tick), then swipe horizontally to glide through tabs with velocity-based liquid squash and stretch. Releasing snaps the ball with harmonic spring physics directly onto the nearest tab.

### ⚡ Official YouTube Shorts Integration
* **Dedicated Shorts Tab**: Instantly access YouTube Shorts directly from the main floating dock.
* **Vertical 9:16 Feed Layout**: Responsive multi-column layout (2 columns in portrait, 4 in landscape) with edge-to-edge vertical thumbnail frames, darkened title gradient scrims, view counts, and quick-action overflow menus.
* **Player Routing**: Seamless one-tap vertical Shorts playback with background audio support.

### 🖤 Pure OLED Black & Soft Premium White Theming
* **True Pure Black (#000000)**: Completely free of dull blueish or muddy gray undertones. Ideal for AMOLED displays with zero idle battery consumption and maximum contrast (`#000000` background, `#0C0C0C` / `#121212` elevated cards and sheets).
* **Soft Premium White (#F8F8FA)**: A soothing, eye-friendly off-white palette for Light mode that maintains clean readability without harsh glare.
* **Live Theme Transitions**: Real-time color evaluation and smooth cross-fading when switching themes.

### ✨ Tactile Interactions & Spring Animations
* **Interactive Action Feedback**:
  * **Video Like**: Tapping thumb-up triggers an overshoot scale bounce (`1.32x -> 1.0x`) with radiant Comet Blue active tinting.
  * **Subscription Toggle**: Spring compression bounce (`0.92x -> 1.06x -> 1.0x`) with smooth background tint morphing.
  * **Comment Likes**: Animated pop and accent color highlights on comments.
* **Haptic Engine**: Tactile tick and click responses for gestures, scrubbing, and tab navigation.

### 👤 Redesigned 'You' Page & Collapsible Explore Menu
* **Clean 3-Tab Dock**: The bottom navbar stays clean with **Home**, **Shorts**, and **You**.
* **Comet Settings**: Accessible right below the user account card.
* **Personal Hub**: Subscriptions, History, Downloads, and Playlists organized right at the top of the You section.
* **Collapsible Explore Menu**: Explore topics (Gaming, Music, Live, News, etc.) are neatly tucked inside an expandable card with an animated 90° rotating chevron dropdown.

### 🚀 First-Time Setup & Onboarding Flow
* **Cinematic Intro**: The Comet logo slides smoothly from the top to center with an overshoot spring curve. 1 second later, "welcome to", the futuristic stylized "COMET" logotype, and "Tube" fade in, followed by the pill-shaped liquid glass "Proceed" button.
* **Interactive Theme Picker**: Real-time background preview between Pure Black and Soft White.
* **Rotating Globe Language Selector**: Beautiful continuously rotating 360° globe icon with a grouped, flag-paired language menu matching iOS table styling.
* **Seamless Entry**: Fades to a pure blank screen where an elegant `"Welcome."` text fades in and out before revealing the Home feed.

### 🔒 Built-in Ad-Free & Content Filtering
* **SponsorBlock**: Automatic skipping of sponsor segments, intros, outros, and subscription reminders.
* **DeArrow**: Replaces clickbait titles and sensationalist thumbnails with community-curated alternatives.
* **Return YouTube Dislike**: Displays authentic estimated dislike counts.
* **Background & PiP Playback**: Picture-in-Picture window and screen-off background playback.
* **Offline Video Downloads**: Save videos up to 1080p or extract audio directly for offline listening.

---

## Liquid Glass Optical Physics

The floating navigation bar uses optical physics implemented via hardware shaders:

```
┌─────────────────────────────────────────────────────────────┐
│                 Scrolled Background Content                 │
└─────────────────────────────────────────────────────────────┘
                               │
                       [Backdrop Capture]
                   RenderNode / GraphicsLayer
                               │
                      [Multi-Pass Blur]
             RenderEffect.createBlurEffect (MIRROR)
                               │
               [AGSL RuntimeShader Refraction]
             ┌─────────────────────────────────┐
             │ • Capsule Signed Distance Field │
             │ • Surface Normal via SDF        │
             │ • Inverse-Power Edge Refraction │
             │ • Chromatic Dispersion (R/G/B)  │
             │ • Fresnel Specular Rim Lighting │
             │ • Vibrancy Saturation (+35%)    │
             └─────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│       1:1 iOS-Style Liquid Glass Floating Navigation        │
└─────────────────────────────────────────────────────────────┘
```

---

## Download & Installation

Pre-built APKs for Android 8.0+ (API 26+) are available in the [Releases](https://github.com/techbymarcon/comettube/releases) section:

| Architecture | APK Name | Description |
|:---|:---|:---|
| **ARM64 (v8a)** | `CometTube_arm64.apk` | Recommended for modern Android smartphones and tablets (64-bit). |
| **Universal** | `CometTube_universal.apk` | Universal package compatible with all architectures (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`). |

---

## Building from Source

### Prerequisites
* **Java Development Kit (JDK)**: OpenJDK 17
* **Android SDK**: Build-Tools 35.0.0+ / 36.0.0, API Level 35/37
* **Git**

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/techbymarcon/comettube.git
   cd comettube
   ```

2. Build the mobile release APK:
   ```bash
   ./gradlew :smarttubetv:assembleStmobileDebug
   ```

3. Find the compiled APKs:
   ```bash
   ls smarttubetv/build/outputs/renamed_apks/stmobileDebug/
   ```

---

## Credits & Acknowledgments

* **Developer & Maintainer**: **Marcon** ([@jetemarcon](https://github.com/techbymarcon))
* **CometOS Creator**: **Jxvk** ([@jxvk.comet](https://github.com/jxvk))
* **Upstream Projects**:
  * [NewTube](https://github.com/aleixrodriala/newtube) by [@aleixrodriala](https://github.com/aleixrodriala)
  * [SmartTube](https://github.com/yuliskov/SmartTube) by [@yuliskov](https://github.com/yuliskov)
* **Optical & Shader References**:
  * [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) by [@Kyant0](https://github.com/Kyant0)
  * [Haze](https://github.com/chrisbanes/haze) by [@chrisbanes](https://github.com/chrisbanes)

---

## License

CometTube is licensed under the [Apache License 2.0](LICENSE).  
SmartTube components remain under their respective GNU General Public License v3.0 / MIT licenses.
