# AdaptiveHz — Smart 120Hz & Battery Optimizer for Samsung Galaxy

<div align="center">

[![Читать на русском](https://img.shields.io/badge/Язык-Русский-red?style=for-the-badge)](README_RU.md)
![Platform](https://img.shields.io/badge/Platform-Android%2010+-00A86B?style=for-the-badge&logo=android&logoColor=white)
![Samsung One UI](https://img.shields.io/badge/Samsung%20One%20UI-2.5%20--%206.1+-1428A0?style=for-the-badge&logo=samsung&logoColor=white)
![Permissions](https://img.shields.io/badge/Internet-0%20Permissions%20(100%25%20Private)-success?style=for-the-badge)
![Ecosystem](https://img.shields.io/badge/Ecosystem-HealthCraft.pro-12A170?style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)

<p align="center">
  <b>Intelligent dynamic refresh rate, 120Hz in Power Saving Mode, and up to +45% battery life on non-LTPO Samsung displays.</b>
</p>

[**Download APK (Releases)**](https://github.com/shepard639/AdaptiveHz/releases) • [**Installation Guide**](#-quick-start--setup) • [**About HealthCraft**](#-healthcraft-ecosystem) • [**🇷🇺 Читать на русском**](README_RU.md)

</div>

---

> ### 🌿 [HealthCraft](https://healthcraft.pro/?ref=adaptivehz) Ecosystem
> **AdaptiveHz** is created and maintained as an open-source utility by the **[HealthCraft Team](https://healthcraft.pro/?ref=adaptivehz)**.  
> Our overarching philosophy is mindful energy stewardship.  
> 📱 **AdaptiveHz** optimizes your smartphone's battery endurance,  
> 🧘 **HealthCraft** helps optimize and elevate your personal body energy: intelligent AI food tracking by photo, macronutrients, workouts, and 100% zero-knowledge medical privacy.  
> 👉 **Try it free in your browser or on Android:** [healthcraft.pro](https://healthcraft.pro/?ref=adaptivehz)

---

## ⚡ Overview

On Samsung Galaxy devices with fixed-rate AMOLED displays (Galaxy S20, S20 FE, S21, S21 FE, A52, A53, A54, A73, Galaxy Tab series, etc.), the stock One UI firmware offers only two rigid choices:
1. **Fixed 120 Hz**: Consumes battery rapidly even while looking at static text or reading articles.
2. **Power Saving Mode**: Samsung **forces the panel down to 60 Hz**, destroying the smooth flagship experience.

**AdaptiveHz resolves both bottlenecks:**
- Restores **smooth 120 Hz during Power Saving Mode** in One UI.
- Dynamically ramps to **120 Hz upon touches and scrolls**, seamlessly downshifting to **60 Hz when reading** (extending screen-on time by up to **+45%**).
- **Per-App Profiles**: Automatically enforces 60 Hz for video apps (YouTube, streaming services, media players) where 120 Hz wastes energy.
- **Zero Internet Permissions**: The app does **not** declare `android.permission.INTERNET`. It is physically incapable of transmitting telemetry or user data across the network.
- **Bilingual Interface**: Native runtime switching between English and Russian in both the app and onboarding wizard.

---

## 🌟 Key Features

| Feature | Description |
| :--- | :--- |
| **🚀 Power Saving Bypass (PSM)** | Unlock silky 120 Hz even when Samsung One UI Power Saving Mode is activated. |
| **🌱 Dynamic Touch Adaptivity** | Boosts to 120 Hz on touch/gesture; drops to 60 Hz when idle (timeout customizable from 1 to 10s). |
| **🎬 Per-App Profiles** | Auto-locks to 60 Hz in video streaming and playback apps where higher refresh rate is unnecessary. |
| **🔋 Smart Charging Lock** | Locks at 120 Hz continuously while the device is plugged into a charger. |
| **🛡️ Low Battery Defender** | Automatically locks to 60 Hz system-wide below 20% battery to keep you connected. |
| **🌡️ Thermal Guard** | Temporarily caps to 60 Hz when battery temp exceeds 41.5°C to prevent throttling and degradation. |
| **📊 Status Bar Hz Counter** | Ultra-compact live refresh rate badge (`120ᴴᶻ` / `60ᴴᶻ`) next to the system clock. |
| **🎛️ Movable Floating HUD** | Lightweight floating pill overlay indicating current SurfaceFlinger frame rate. |

---

## 📲 Quick Start & Setup

To manage Android's display refresh rate tables, Android requires a one-time grant of `WRITE_SECURE_SETTINGS`.

### Method 1: On-device via Shizuku (Recommended, No PC needed)
*No computer required. Takes 1 minute over local Wi-Fi.*

1. Install [Shizuku from Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api).
2. Open phone **Settings** -> **Developer Options** -> enable **Wireless Debugging**. *(If Developer mode is not enabled yet: Settings -> About Phone -> Software Information -> tap "Build Number" 7 times).*
3. In Shizuku, tap **Pairing**, enter the 6-digit code from notifications and start the Shizuku service.
4. Launch **AdaptiveHz** and tap **"Grant via Shizuku"**. Done!

### Method 2: Via PC with USB Cable
1. Enable **USB Debugging** in **Developer Options**.
2. Connect your phone to PC and execute in terminal (PowerShell, Command Prompt, or Terminal):
   ```bash
   adb shell pm grant com.healthcraft.adaptivehz android.permission.WRITE_SECURE_SETTINGS
   ```
3. Done! The permission persists across device reboots and app updates.

---

## 🔄 App Updates

Because AdaptiveHz operates strictly offline (`0 network permissions`) for absolute privacy, you can update it using either of these two methods:

1. **Manual In-App Link**: Tap **"Check Updates on GitHub"** in the *About* tab of the app — this opens the official [Releases page](https://github.com/shepard639/AdaptiveHz/releases) where signed APKs are published.
2. **Automated via Obtainium (Recommended)**:
   For automated background updates without Google Play or app stores, use the open-source update manager [Obtainium](https://github.com/ImranR98/Obtainium):
   - Copy this repo URL: `https://github.com/shepard639/AdaptiveHz`
   - Paste into Obtainium -> **Add App**.
   - Obtainium will notify and update AdaptiveHz seamlessly.

---

## 🛡️ Security & Architecture

- **Zero Internet Permission**: No network access is requested or possible. 100% offline, local execution.
- **In-Memory IPC Caching**: The display controller caches mode states to prevent redundant SQLite writes to `system_settings`, eliminating micro-stutters and conserving CPU cycles.
- **DisplayListener Synchronization**: Directly bound to Android's hardware display manager, ensuring the HUD and status bar reflect authentic SurfaceFlinger refresh rates.

---

## 🛠️ Building from Source

Developed in Kotlin using Jetpack Compose and modern Android architecture.

```bash
# Clone repository
git clone https://github.com/shepard639/AdaptiveHz.git
cd AdaptiveHz

# Build Debug APK
./gradlew assembleDebug

# Execute unit tests
./gradlew testDebugUnitTest
```

---

## ⚖️ Legal Disclaimer

- **Trademarks**: Samsung, Galaxy, and One UI are registered trademarks of Samsung Electronics Co., Ltd. This project is an independent open-source development and is not affiliated with, endorsed by, or sponsored by Samsung.
- **Disclaimer of Warranty**: This application is provided "AS IS", without warranty of any kind. The developers assume no liability for any operating system issues, unexpected behaviors, or third-party app anomalies resulting from forced display refresh rate changes.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](./LICENSE).

---

<div align="center">
  <sub>Developed by the <b>HealthCraft Team</b> with modern AI-assisted engineering practices.</sub><br>
  <sub>Crafted with energy mindfulness inside the <a href="https://healthcraft.pro/?ref=adaptivehz">HealthCraft</a> ecosystem.</sub>
</div>
