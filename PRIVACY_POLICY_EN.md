# Privacy Policy for AdaptiveHz

**Effective Date**: October 10, 2026  
**Developer**: HealthCraft Team  
**Application**: AdaptiveHz — Smart Refresh Rate & Battery Optimization for Samsung Galaxy  
**Project**: [AdaptiveHz on GitHub](https://github.com/shepard639/AdaptiveHz) • [HealthCraft Ecosystem](https://healthcraft.pro)  
**Russian Version**: [Политика конфиденциальности (RU)](./PRIVACY_POLICY.md)

---

### 1. General Principles
This Privacy Policy governs the processing and protection of user information within the **AdaptiveHz** mobile application (hereinafter referred to as the "Application").

The developer respects every user's fundamental right to privacy and upholds an absolute privacy architecture: **The Application does not collect, store, process, or transmit to any third parties any personal or identifiable information whatsoever**.

---

### 2. Information Collection and Processing
1. **Zero Internet Permission**:
   The Application manifest (`AndroidManifest.xml`) does not declare `android.permission.INTERNET`. The Application is completely disconnected and isolated from the global internet and cannot make outbound network calls, receive incoming traffic, or transmit telemetry, tracking, or crash analytics.
2. **Personal Data**:
   The Application does not request or process names, phone numbers, email addresses, precise or coarse location, contacts, media files, passwords, or financial data.
3. **Local Settings Storage**:
   All user preferences (selected refresh rate mode, idle drop timeouts, per-app profiles) are stored strictly locally on your device within Android's protected DataStore Preferences sandbox and are completely removed when the Application is uninstalled.

---

### 3. System Permissions and Their Purpose
To execute its display management capabilities, the Application requires the following limited Android system permissions:

| Permission | Purpose |
| :--- | :--- |
| `WRITE_SECURE_SETTINGS` | **Core permission**. Used exclusively to adjust system display refresh rate parameters (`peak_refresh_rate`, `min_refresh_rate`, `refresh_rate_mode`) within Android and Samsung One UI system settings tables. Granted by the user manually via Shizuku on-device (no PC required) or via computer using an ADB command. |
| `BIND_ACCESSIBILITY_SERVICE` | **Accessibility Service**. Used strictly to detect user touch gestures (`TYPE_TOUCH_INTERACTION_START` / `END`) and scroll events to instantly elevate refresh rate to 120 Hz, as well as detecting the foreground app package name to activate battery-saving profiles (e.g. 60 Hz for video players). The Application **does not inspect** text, keystrokes, passwords, or sensitive on-screen content. |
| `POST_NOTIFICATIONS` | Used to display real-time refresh rate indicators (`60ᴴᶻ` / `120ᴴᶻ`) in the status bar and provide quick-toggle action buttons in the notification drawer. |
| `FOREGROUND_SERVICE` (`specialUse`) | Keeps the status bar monitor active in memory without being abruptly terminated by system memory management. |
| `QUERY_ALL_PACKAGES` | Used solely to enumerate installed apps in the "App Profiles" tab so users can customize frame rates per application. No installed app lists are ever exported or transmitted off-device. |

---

### 4. Data Security
The Application executes strictly inside the standard Android sandbox. All calculations and operations occur entirely on the local device CPU. The entire source code is open source and available for public review and independent audit on GitHub.

---

### 5. Legal Information and Disclaimers
1. **Trademarks**:
   Samsung, Galaxy, and One UI are registered trademarks of Samsung Electronics Co., Ltd. This project is an independent open-source development and is not affiliated with, endorsed by, or sponsored by Samsung.
2. **Disclaimer of Warranty**:
   The Application is provided "AS IS", without warranty of any kind. Developers assume no liability for any operating system anomalies, third-party app incompatibilities, or hardware issues resulting from forced display refresh rate adjustments.

---

### 6. Contact and Inquiries
For questions, feedback, or inquiries regarding the Application or this Privacy Policy:
- [GitHub Issues](https://github.com/shepard639/AdaptiveHz/issues)
- [HealthCraft Ecosystem](https://healthcraft.pro)
