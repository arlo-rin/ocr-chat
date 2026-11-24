<div align="center">
  <img src="docs/icon.png" alt="SideReply Logo" width="100" height="100">

  <h1>侧答 · SideReply</h1>

  <p>
    <strong>面向 Android 11+ 的微信 AI 回复助手（Root 版）——基于 Jetpack Compose 与 Material Design 3，通过内存 Root 截屏直连视觉大模型，零无障碍服务。</strong>
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

* **Material Design 3 / Material You 全新重构** — 主界面采用 M3 `LargeTopAppBar` + 三标签 `NavigationBar`（概览 / 模型 / 偏好），支持浅色、深色、跟随系统及 Android 12+ 壁纸动态取色，并将主题实时同步至悬浮球与悬浮面板。
* **可交互的概览控制台** — 首页提供动态色调主控大卡、运行态一键切换显示范围芯片（`仅在微信显示` / `所有应用可用`）、权限与 AI 引擎双列 Bento 状态卡、支持切换语气的实景预览，以及随时可查的 Root 命令清单底部弹窗。
* **零无障碍 Root 内存截屏** — 不申请 `BIND_ACCESSIBILITY_SERVICE`；点击侧边悬浮球后先移除自身悬浮窗，再通过 `su -c screencap -p` 从 stdout 读入内存并压缩为最长边 1600px 的 JPEG（不写磁盘、不入相册），直传支持 `image_url` 的视觉模型。
* **双重前台感知 + 应用双开支持** — 以 `UsageStatsManager` 为主路径识别前台包名，在多用户双开设备或 `UsageStats` 漏报切换时自动通过 Root `WindowFocusProbe`（`dumpsys window displays`）兜底识别主用户与分身窗口（如 `u999`）。
* **Compose M3 悬浮球与建议面板** — 悬浮球支持按压缩放与自动贴边；点击后在 `WindowManager` 中加载 Compose M3 底部面板，提供可折叠多轮 AI 对话记录、三色调建议卡片、轻触复制、底部拇指区快捷语气 `FilterChip` 与自定义风格「换一组」。
* **视觉服务预设与本机密钥加密** — 内置 OpenAI、阿里云通义、智谱 GLM、Kimi 与本地调试 Mock 快捷预设；API Key 采用 Android Keystore `AES/GCM/NoPadding` 加密存储，并禁用云备份与明文 HTTP（本地调试回环除外）。

---

## 📸 Screenshots

<p align="center">
  <img src="docs/01-main.png" width="260" alt="概览首页（Material You 浅色）" />
  <img src="docs/02-settings-dark.png" width="260" alt="偏好与外观（深色）" />
  <img src="docs/03-root-sheet.png" width="260" alt="Root 命令透明清单" />
</p>

---

## 🚀 Quick Start

1. **安装 APK**：从 [Releases](https://github.com/arlo-rin/ocr_chat/releases) 下载或按下方步骤从源码编译安装 Debug APK（需 **Android 11+** 与可用的 Root 管理器，如 Magisk / KernelSU）。
2. **启动悬浮球**：打开「侧答」，在 **概览** 页点击 **`启动悬浮球`**。首次启动缺少权限时，侧答会请求 Root 并通过 `appops` 自动授予 `SYSTEM_ALERT_WINDOW` 与 `GET_USAGE_STATS`（已有权限时不再弹窗请求 Root）。
3. **配置视觉模型**：切换到 **模型** 页，点选顶部预设（或填写兼容 OpenAI `POST /chat/completions` 的地址、模型与 API Key），点击 **`保存设置`** 后再用 **`测试连接`** 验证。
4. **在微信中使用**：打开微信聊天界面（悬浮球在「侧答」本页与桌面会自动隐藏；如需在其他应用调试，可在概览页点 **`所有应用可用`**），轻点侧边悬浮球即可在底部 M3 面板中一键复制建议；想换语气可点选底部风格标签或输入要求后点 **`换一组`**。

> **注意**：请务必配置**支持图片输入（`image_url`）的视觉模型**。`测试连接` 仅发送简短纯文本验证地址与密钥，若所选模型不支持图片，首次点击悬浮球识图时会返回 400 错误提示。

---

## 🔐 Root Commands

侧答不启动常驻 root shell，也不拼接用户输入到命令中，所有 Root 操作固定为以下三类（可在 App 内 **偏好 → Root 执行命令清单** 查看）：

| 场景 | 执行命令 | 说明 |
| --- | --- | --- |
| 首次权限授权 | `appops set app.sidereply SYSTEM_ALERT_WINDOW allow`<br>`appops set app.sidereply GET_USAGE_STATS allow`<br>`id` | 仅当缺少悬浮窗或使用情况权限时调用，`id` 用于校验 `uid=0`。 |
| 点击悬浮球截屏 | `su -c screencap -p` | PNG 经 stdout 读入内存并即时解码回收，不落盘（兼容 `su 0 sh -c`）。 |
| 焦点窗口探测 | `pm list users`<br>`dumpsys window displays \| grep -m 6 -E 'mCurrentFocus=\|mFocusedApp='` | 仅在多用户双开设备或 `UsageStats` 未捕获应用切换时触发，自动过滤 `=null` 副屏行。 |

---

## 🤖 Model Setup

请求采用 OpenAI 兼容格式，自动规范化 Base URL 与 `/chat/completions` 路径。为保障隐私，仅允许 `https://` 与本机调试地址（`10.0.2.2`、`localhost`、`127.0.0.1`）：

| 服务预设 | 接口地址 | 视觉模型示例 |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1/chat/completions` | `gpt-4o-mini` |
| 阿里云通义（视觉） | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | `qwen-vl-plus` |
| 智谱 GLM（视觉） | `https://open.bigmodel.cn/api/paas/v4/chat/completions` | `glm-4v-flash` |
| Moonshot / Kimi（视觉） | `https://api.moonshot.cn/v1/chat/completions` | `moonshot-v1-8k-vision-preview` |
| 本地调试 Mock（模拟器） | `http://10.0.2.2:8787/v1/chat/completions` | `mock-chat` |

### 本地 Mock 服务调试

仓库内置零依赖测试服务 [tools/mock-ai-server.js](tools/mock-ai-server.js)，可在无 API Key 时调试 UI 与截图上传流程（演示聊天页 `http://10.0.2.2:8787/demo`）：

```powershell
node tools/mock-ai-server.js
```

---

## 🛠️ Build & Test

* **构建工具链**：AGP `9.4.1` · Gradle `9.6.0` · Kotlin Compose Compiler `2.2.10` · `compileSdk = 37` · `minSdk = 30` · `targetSdk = 36` · JDK `17+`
* 请确保根目录 `local.properties` 中 `sdk.dir` 指向本机 Android SDK。

```powershell
# 运行 JVM 单元测试
.\gradlew.bat :app:testDebugUnitTest

# 运行 Android Lint
.\gradlew.bat :app:lintDebug

# 编译 Debug APK
.\gradlew.bat :app:assembleDebug

# 安装到已连接的设备或模拟器
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

---

## 🧱 Architecture

```text
app/src/main/java/app/sidereply/
├── MainActivity.kt                      Compose 入口、前台状态追踪、Root 授权与连接测试
├── ai/
│   ├── AiClient.kt                      OpenAI 兼容 HttpURLConnection 客户端（含 image_url 请求体）
│   ├── AiErrorMessages.kt               网络与接口错误中文提示映射
│   ├── EndpointNormalizer.kt            Base URL 自动补全与 HTTPS/回环安全校验
│   ├── Models.kt                        AiConfig / ReplySuggestion / AiException
│   ├── ProviderPresets.kt               主流视觉大模型预设与 host 匹配
│   ├── ReplyParser.kt                   JSON / 代码块 / 纯文本宽容解析
│   └── ReplyPrompts.kt                  「我」发送者视角锁定、指定风格与换一组提示词
├── data/
│   ├── AppPreferences.kt                SharedPreferences 封装（显示范围、双开、主题模式、动态取色）
│   └── SecretStore.kt                   Android Keystore AES-GCM 加解密
├── root/
│   ├── RootManager.kt                   appops 授权与权限检查
│   ├── RootShell.kt                     su 命令执行与 stdout 内存截屏
│   └── WindowFocusProbe.kt              dumpsys window 焦点窗口解析（多屏与双开兼容）
├── service/
│   └── FloatingAssistantService.kt      前台服务、UsageStats/Root 前台识别、M3 悬浮球与 Compose 悬浮窗
└── ui/
    ├── SheetRootLayout.kt               悬浮窗根视图（拦截系统返回键关闭面板）
    ├── icons/SideReplyIcons.kt          Material Symbols 风格矢量图标集
    ├── main/
    │   ├── MainUiState.kt               主界面标签与配置状态
    │   ├── SideReplyApp.kt              M3 Scaffold / LargeTopAppBar / NavigationBar / 弹窗
    │   ├── HomeTab.kt                   概览页（主控卡、范围切换芯片、Bento 状态卡、实景预览）
    │   ├── ModelTab.kt                  模型页（预设芯片、校验表单、密钥显示/清除、连接测试）
    │   └── SettingsTab.kt               偏好页（分组圆角卡片、深浅主题、动态取色、隐私说明）
    ├── sheet/
    │   ├── OverlayLifecycleOwner.kt     WindowManager 悬浮窗内嵌 Compose 生命周期宿主
    │   ├── ReplySheetUiState.kt         悬浮面板多轮对话与阶段状态
    │   └── ReplySheetContent.kt         Compose M3 底部建议面板、对话记录与快捷风格栏
    └── theme/
        ├── Color.kt                     Material 3 浅色 / 深色 HCT 色板
        ├── Shape.kt                     M3 形状与分组圆角规范
        ├── Type.kt                      M3 排版层级
        └── Theme.kt                     SideReplyTheme（支持动态取色与安全回退）
```

---

## ⚠️ Notes & Permissions

* **声明权限**：`INTERNET`、`SYSTEM_ALERT_WINDOW`、`PACKAGE_USAGE_STATS`、`FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_SPECIAL_USE`、`POST_NOTIFICATIONS`；**无 `BIND_ACCESSIBILITY_SERVICE`**，不申请存储、相机、通讯录或开机自启权限。
* **隐私与安全**：整张截图仅在内存中压缩为 JPEG 后发送至你配置的 AI 服务，关闭面板即释放内存引用；若当前聊天窗口开启了 `FLAG_SECURE`，系统截屏可能返回黑屏。
* **免责声明**：本项目为个人工具原型，不隶属于腾讯/微信或任何 Root 管理器；使用 Root 权限与第三方视觉模型前请自行审阅源码并评估对话隐私。
