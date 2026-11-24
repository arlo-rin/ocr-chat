<div align="center">
  <img src="docs/icon.png" alt="SideReply Logo" width="100" height="100">

  <h1>SideReply · 侧答</h1>

  <p>
    <strong>A Root-powered AI reply assistant for WeChat on Android 11+ — built with Jetpack Compose &amp; Material Design 3, sending in-memory root screenshots directly to vision models with zero Accessibility Services.</strong>
  </p>

  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README_en.md">English</a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Version-v0.5.1--root-006A60.svg" alt="Version">
    <img src="https://img.shields.io/badge/Platform-Android%2011%2B-3DDC84.svg" alt="Platform">
    <img src="https://img.shields.io/badge/Built%20with-Jetpack%20Compose%20%26%20Material%203-4285F4.svg" alt="Built with">
    <img src="https://img.shields.io/badge/Accessibility-None%20(Root%20Only)-success.svg" alt="Zero Accessibility">
  </p>

  <p>
    <a href="#-highlights">Highlights</a> ·
    <a href="#-screenshots">Screenshots</a> ·
    <a href="#-quick-start">Quick Start</a> ·
    <a href="#-root-commands">Root Commands</a> ·
    <a href="#-model-setup">Model Setup</a> ·
    <a href="#-build--test">Build &amp; Test</a> ·
    <a href="#-architecture">Architecture</a>
  </p>
</div>

---

## ✨ Highlights

* **Material Design 3 & Material You** — Redesigned with an M3 `LargeTopAppBar` and a three-tab `NavigationBar` (Overview / Model / Settings), supporting Light, Dark, System themes, and Android 12+ dynamic wallpaper color synced live to the floating orb and reply sheet.
* **Interactive Overview Dashboard** — Features a state-aware hero control card, one-tap visibility scope chips (`WeChat Only` / `All Apps`), dual Bento status cards for permissions & AI engine, an interactive tone-switching reply preview, and a transparent Root commands bottom sheet.
* **Zero-Accessibility In-Memory Root Capture** — Declares no `BIND_ACCESSIBILITY_SERVICE`; tapping the floating orb temporarily hides the overlay, runs `su -c screencap -p` into memory, scales to max 1600px JPEG (never written to disk or gallery), and sends it directly to any vision-capable `image_url` model.
* **Hybrid Foreground Detection + Dual-App Support** — Primarily tracks foreground apps via `UsageStatsManager`, and automatically falls back to Root `WindowFocusProbe` (`dumpsys window displays`) on multi-user clone devices (e.g., `u999`) or whenever `UsageStats` misses an app switch.
* **Compose M3 Floating Orb & Reply Sheet** — Draggable 34dp floating orb with press scaling and edge snapping; tapping opens a Compose M3 overlay bottom sheet with an expandable multi-round AI conversation log, three tonal suggestion cards, one-tap copy, and a thumb-zone style refinement bar.
* **Provider Presets & On-Device Key Encryption** — Built-in presets for OpenAI, Alibaba Qwen VL, Zhipu GLM, Moonshot Kimi, and local mock debugging; API keys are encrypted via Android Keystore `AES/GCM/NoPadding` with cloud backup and cleartext HTTP disabled (except loopback hosts).

---

## 📸 Screenshots

<p align="center">
  <img src="docs/01-main.png" width="260" alt="Overview Home (Material You Light)" />
  <img src="docs/02-settings-dark.png" width="260" alt="Settings & Appearance (Dark)" />
  <img src="docs/03-root-sheet.png" width="260" alt="Root Commands Transparency Sheet" />
</p>

---

## 🚀 Quick Start

1. **Install the APK**: Download from [Releases](https://github.com/arlo-rin/ocr_chat/releases) or build from source below (requires **Android 11+** and a working Root manager such as Magisk or KernelSU).
2. **Start the Floating Orb**: Open SideReply and tap **`启动悬浮球`** (Start Floating Orb) on the **Overview** tab. If permissions are missing, SideReply automatically grants `SYSTEM_ALERT_WINDOW` and `GET_USAGE_STATS` via `appops` through Root (subsequent starts skip Root if already granted).
3. **Configure a Vision Model**: Open the **Model** tab, pick a preset or enter an OpenAI-compatible `POST /chat/completions` endpoint, vision model name, and API key, then tap **`保存设置`** (Save) and **`测试连接`** (Test Connection).
4. **Use in WeChat**: Switch to a WeChat chat screen (the orb automatically hides inside SideReply itself and on the launcher; tap **`所有应用可用`** on the Overview card if you want to test in other apps). Tap the floating orb, wait for vision analysis, tap any suggestion to copy, or pick a tone chip and regenerate.

> **Important**: You must choose a **vision-capable model supporting `image_url`**. The built-in connection test sends a short text-only prompt to verify endpoint, model name, and key; if your model does not accept images, the endpoint will return HTTP 400 when tapping the orb.

---

## 🔐 Root Commands

SideReply never keeps a persistent root shell open and never interpolates user input into shell commands. All Root invocations are fixed in code and inspectable inside the app under **Settings → Root Commands**:

| Purpose | Command | Details |
| --- | --- | --- |
| One-time Permission Setup | `appops set app.sidereply SYSTEM_ALERT_WINDOW allow`<br>`appops set app.sidereply GET_USAGE_STATS allow`<br>`id` | Executed only if overlay or usage access is missing; `id` verifies `uid=0`. |
| In-Memory Screenshot | `su -c screencap -p` | PNG bytes are read from stdout into memory, compressed to JPEG, and recycled (also supports `su 0 sh -c`). |
| Focus Window Probe | `pm list users`<br>`dumpsys window displays \| grep -m 6 -E 'mCurrentFocus=\|mFocusedApp='` | Triggered only when dual-space users exist or `UsageStats` has not reported a switch; skips `=null` secondary displays. |

---

## 🤖 Model Setup

Requests follow the OpenAI Chat Completions contract, normalizing base URLs automatically. Only `https://` endpoints and local loopback hosts (`10.0.2.2`, `localhost`, `127.0.0.1`) are permitted:

| Preset | Endpoint | Sample Vision Model |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1/chat/completions` | `gpt-4o-mini` |
| Alibaba Qwen (Vision) | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | `qwen-vl-plus` |
| Zhipu GLM (Vision) | `https://open.bigmodel.cn/api/paas/v4/chat/completions` | `glm-4v-flash` |
| Moonshot / Kimi (Vision) | `https://api.moonshot.cn/v1/chat/completions` | `moonshot-v1-8k-vision-preview` |
| Local Mock (Emulator) | `http://10.0.2.2:8787/v1/chat/completions` | `mock-chat` |

### Local Mock Server

A zero-dependency Node.js mock server is included at [tools/mock-ai-server.js](tools/mock-ai-server.js) for UI and upload testing without an API key (includes a demo chat page at `http://10.0.2.2:8787/demo`):

```powershell
node tools/mock-ai-server.js
```

---

## 🛠️ Build & Test

* **Toolchain**: AGP `9.4.1` · Gradle `9.6.0` · Kotlin Compose Compiler `2.2.10` · `compileSdk = 37` · `minSdk = 30` · `targetSdk = 36` · JDK `17+`
* Ensure `local.properties` points `sdk.dir` to your local Android SDK.

```powershell
# Run JVM unit tests
.\gradlew.bat :app:testDebugUnitTest

# Run Android Lint
.\gradlew.bat :app:lintDebug

# Build Debug APK
.\gradlew.bat :app:assembleDebug

# Install onto connected device or emulator
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

---

## 🧱 Architecture

```text
app/src/main/java/app/sidereply/
├── MainActivity.kt                      Compose entry, foreground tracking, Root setup & connection test
├── ai/
│   ├── AiClient.kt                      OpenAI-compatible HttpURLConnection client with image_url payload
│   ├── AiErrorMessages.kt               Error-to-message mapper
│   ├── EndpointNormalizer.kt            Base URL normalization & HTTPS/loopback validation
│   ├── Models.kt                        AiConfig / ReplySuggestion / AiException
│   ├── ProviderPresets.kt               Vision provider presets & host matcher
│   ├── ReplyParser.kt                   Lenient JSON / fenced block / plain-text parser
│   └── ReplyPrompts.kt                  Sender-perspective ("I") constraint & style/regeneration prompts
├── data/
│   ├── AppPreferences.kt                SharedPreferences wrapper (scope, dual-app, theme mode, dynamic color)
│   └── SecretStore.kt                   Android Keystore AES-GCM encryption
├── root/
│   ├── RootManager.kt                   appops permission grants & readiness checks
│   ├── RootShell.kt                     su execution & stdout in-memory screenshot capture
│   └── WindowFocusProbe.kt              dumpsys window focus parser (multi-display & dual-space)
├── service/
│   └── FloatingAssistantService.kt      Foreground service, UsageStats/Root probing, M3 orb & Compose sheet
└── ui/
    ├── SheetRootLayout.kt               Overlay root container handling system Back events
    ├── icons/SideReplyIcons.kt          Material Symbols vector icon set
    ├── main/
    │   ├── MainUiState.kt               Main screen tabs & form state
    │   ├── SideReplyApp.kt              M3 Scaffold / LargeTopAppBar / NavigationBar / dialogs
    │   ├── HomeTab.kt                   Overview tab (hero control, scope chips, Bento cards, live preview)
    │   ├── ModelTab.kt                  Model tab (provider presets, validation, key controls, test action)
    │   └── SettingsTab.kt               Settings tab (grouped cards, theme controls, privacy & Root sheet)
    ├── sheet/
    │   ├── OverlayLifecycleOwner.kt     Lifecycle/SavedState/ViewModelStore owner for Service ComposeView
    │   ├── ReplySheetUiState.kt         Overlay sheet rounds & phase state
    │   └── ReplySheetContent.kt         Compose M3 reply bottom sheet, chat log & style bar
    └── theme/
        ├── Color.kt                     Material 3 Light / Dark HCT palettes
        ├── Shape.kt                     M3 shapes & grouped card corner tokens
        ├── Type.kt                      M3 typography scale
        └── Theme.kt                     SideReplyTheme (dynamic color with safe fallback)
```

---

## ⚠️ Notes & Permissions

* **Permissions**: `INTERNET`, `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`. **No `BIND_ACCESSIBILITY_SERVICE`**, and no storage, camera, contacts, or boot-completed permissions.
* **Privacy & Security**: Full-screen JPEG snapshots exist only in memory during the request and are cleared when the sheet closes; screens marked `FLAG_SECURE` may capture black frames.
* **Disclaimer**: Personal utility prototype, not affiliated with Tencent/WeChat or any Root manager. Review the source code before granting Root privileges and verify AI replies before sending.
