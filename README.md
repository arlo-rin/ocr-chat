# 侧答 · SideReply（Root 版）

> 在微信里轻点侧边悬浮球，侧答通过 Root 截屏看懂当前聊天，在 Material Design 3 底部面板里递给你三条可直接发送的回复。

「侧答」是一个面向 **Android 11+** 的微信 AI 回复助手（`v0.5.0-root`）。**它不使用无障碍服务**，也不内置任何本地 OCR 或机器学习模型。应用基于 **Jetpack Compose + Material Design 3（Material You）** 构建，配合已 Root 设备通过 `su` 一次性授权悬浮窗与使用情况访问权限，用 `UsageStats`（以及双开设备上的焦点窗口探测）判断前台应用；点击悬浮球时通过 `su -c screencap -p` 在内存中截屏并压缩为 JPEG，直接交给支持视觉输入的 OpenAI 兼容模型理解对话并生成三条不同语气的回复建议。

<p align="center">
  <img src="docs/icon.png" width="96" alt="侧答图标" />
</p>

<p align="center">
  <img src="docs/01-main.png" width="270" alt="概览首页（浅色）" />
  <img src="docs/02-settings-dark.png" width="270" alt="偏好与外观（深色）" />
  <img src="docs/03-root-sheet.png" width="270" alt="Root 命令透明清单" />
</p>

## 核心特性

### 1. Material Design 3 与 Material You 体验
- **三标签信息架构**：主界面采用 M3 `LargeTopAppBar` + `NavigationBar`，清晰拆分为 **概览**、**模型**、**偏好** 三个页面：
  - **概览**：服务状态主控大卡（未启动 / 配置中 / 运行中动态变色）、双列 Bento 状态卡片（悬浮窗与使用情况权限状态、当前 AI 引擎概览）、可切换语气并一键复制的交互式实景预览、三步上手指引。
  - **模型**：横向识图模型快捷预设（OpenAI、阿里云通义、智谱 GLM、Kimi、本地调试）、`OutlinedTextField` 表单校验与内联测试反馈、API Key 按需解密查看与二次确认清除。
  - **偏好**：M3 Expressive 分组圆角卡片与勾选开关，控制微信专属显示、应用双开支持、深浅主题（跟随系统 / 浅色 / 深色）、**Material You 动态取色**，以及可随时查阅的 **Root 命令透明清单**弹窗。
- **动态取色悬浮球**：默认在目标应用侧边贴附 34dp 圆形悬浮球（48dp 触控热区，内嵌星芒图标），配色实时跟随当前 Material 3 主题，支持按压缩放反馈、自由拖拽与松手自动吸附左右边缘。
- **Compose M3 悬浮建议面板**：在服务窗口内直接承载 Compose Material 3 底部面板，支持顶栏拖动手柄下滑关闭、点击遮罩关闭与系统返回键关闭：
  - **可折叠 AI 对话记录**：请求发送后即时在顶部轮次胶囊与对话记录中展示上传截图大小与生成进度；多轮「换一组」会保留完整历史轮次，方便对比。
  - **三条差异化建议卡片**：默认提供「自然 / 稳妥 / 轻松」三种色调容器卡片，轻触整张卡片或复制按钮即可复制到剪贴板并切换为已复制状态。
  - **拇指热区风格调节栏**：底部固定快捷语气 `FilterChip`（更简短 / 温柔 / 严谨 / 幽默 / 礼貌委婉 / 直接一点）与自定义风格输入框，输入或点选后即可触发「换一组」。

### 2. Root 截屏与前台感知（零无障碍）
- **不使用无障碍服务**：`AndroidManifest.xml` 中没有 `BIND_ACCESSIBILITY_SERVICE`，也不读取微信控件树。
- **仅在目标应用出现**：通过 `UsageStatsManager` 检测最近恢复的 Activity，默认仅在微信（`com.tencent.mm`）前台时显示悬浮球，切回桌面或锁屏自动隐藏；可在「偏好」中关闭「仅在微信中显示」以便调试或其他聊天软件使用。
- **支持应用双开 / 应用分身**：系统多用户双开副本对 `UsageStats` 不可见。检测到存在额外 Android 用户且开启「支持应用双开 / 分身」时，服务会通过 Root 探测全局焦点窗口（`WindowFocusProbe`），使双开微信同样能唤出悬浮球；无双开用户的设备完全跳过探测。
- **隐私友好截屏链路**：点击悬浮球后先移除悬浮层再执行 `su -c screencap -p`，确保截图不会拍入侧答自身界面；PNG 直接通过 stdout 读入内存，缩放至最长边 1600px、压成 JPEG（质量 82）后发给视觉模型并立即回收 Bitmap，不写入相册或本地磁盘。
- **发送者视角锁定与换一组**：系统与用户提示词强制要求以手机主人（“我”）视角生成可直接发送的回复，不会替对方接话；「换一组」会复用内存中的同一张截图并携带上一轮建议避免重复。
- **本机密钥加密**：API Key 使用 Android Keystore AES-GCM 加密保存在应用私有偏好中，`allowBackup="false"` 并通过 `data_extraction_rules.xml` 排除云备份与设备迁移。

## 快速上手

1. 安装由源码编译的 Debug APK（见下方构建步骤）。
2. 打开「侧答」，在 **概览** 页点击 **启动悬浮球**：
   - 首次启动若缺少权限，会调用 Root 自动执行 `appops` 授予悬浮窗（`SYSTEM_ALERT_WINDOW`）和使用情况访问（`GET_USAGE_STATS`）权限；已有权限时不会重复弹出 Root 授权。
   - 在 Android 13+ 上建议允许通知权限，以便系统展示「侧答正在运行」常驻前台通知。
3. 切换到 **模型** 页，点选顶部的视觉模型预设或手动填写 **接口地址 / Base URL**、**识图模型名称** 与 **API Key**，点 **保存设置**，再点 **测试连接** 验证链路通畅。
4. 打开微信聊天界面，轻点侧边悬浮球等待模型识图，在底部弹出的 M3 面板中点击任意一条建议复制，回到微信输入框粘贴发送。

> **重要提示**：必须使用**支持图片输入（`image_url`）的视觉模型**。内置的「测试连接」仅发送纯文本以确认地址、模型与密钥可用，无法验证模型是否具备识图能力；若模型不支持图片，第一次点小球时会返回 400 并给出明确提示。

## 从源码构建

### 环境要求
- Android Studio（本项目使用 AGP `9.4.1` / Gradle `9.6.0` / Kotlin Compose Compiler `2.2.10`）
- Android SDK Platform 37（`compileSdk = 37`，`minSdk = 30`，`targetSdk = 36`）
- JDK 17 或更高版本（已在 JDK 17 环境下验证）
- 仓库根目录的 `local.properties` 需指向本机 Android SDK（`sdk.dir=...`）

### 构建与测试命令

Windows PowerShell：

```powershell
# 单元测试
.\gradlew.bat :app:testDebugUnitTest

# Android Lint 检查
.\gradlew.bat :app:lintDebug

# 编译 Debug APK
.\gradlew.bat :app:assembleDebug

# 安装到已连接的设备或模拟器
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

macOS / Linux：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Root 权限与命令透明度

侧答不会启动常驻 root shell，也不拼接任何用户输入或网络内容进入 shell 命令。所有 Root 调用均固化在 [RootShell.kt](app/src/main/java/app/sidereply/root/RootShell.kt)、[RootManager.kt](app/src/main/java/app/sidereply/root/RootManager.kt) 与 [WindowFocusProbe.kt](app/src/main/java/app/sidereply/root/WindowFocusProbe.kt) 中，且可在 App 内「偏好 → Root 执行命令清单」随时查看：

1. **首次权限授予**（仅当缺少对应权限时执行）：
   ```sh
   appops set app.sidereply SYSTEM_ALERT_WINDOW allow
   appops set app.sidereply GET_USAGE_STATS allow
   id
   ```
2. **点击悬浮球内存截屏**：
   ```sh
   su -c screencap -p
   ```
   同时兼容部分 AOSP userdebug 系统的 `su 0 sh -c` 调用形式。
3. **双开焦点窗口探测**（仅当检测到多用户且未关闭双开开关时）：
   ```sh
   pm list users
   dumpsys window displays | grep -m 2 -E 'mCurrentFocus=|mFocusedApp='
   ```

## 配置兼容的视觉模型接口

侧答请求兼容 OpenAI `POST /chat/completions` 规范，`user.content` 为 `[{type:"text"}, {type:"image_url"}]` 数组（`image_url` 为 `data:image/jpeg;base64,...`）。接口地址支持填 Base URL 或完整 `/chat/completions` 地址，保存与测试时会自动补全并回显。

| 服务预设 | 接口地址 | 视觉模型示例 |
| --- | --- | --- |
| OpenAI | `https://api.openai.com/v1/chat/completions` | `gpt-4o-mini` |
| 阿里云通义（视觉） | `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions` | `qwen-vl-plus` |
| 智谱 GLM（视觉） | `https://open.bigmodel.cn/api/paas/v4/chat/completions` | `glm-4v-flash` |
| Moonshot / Kimi（视觉） | `https://api.moonshot.cn/v1/chat/completions` | `moonshot-v1-8k-vision-preview` |
| 本地调试 Mock（模拟器） | `http://10.0.2.2:8787/v1/chat/completions` | `mock-chat` |

> **网络安全限制**：为保护 API Key 与聊天截图，应用强制禁止明文 HTTP（`usesCleartextTraffic="false"`），仅允许 `https://` 与本机/模拟器调试回环地址（`10.0.2.2`、`localhost`、`127.0.0.1`）。

### 使用本地 Mock 服务调试 UI

仓库内置零依赖 Node.js mock 服务 [tools/mock-ai-server.js](tools/mock-ai-server.js)，适合无 API Key 时在模拟器或本机验证端到端请求与面板交互：

```powershell
node tools/mock-ai-server.js
```

- 模拟器接口地址：`http://10.0.2.2:8787/v1/chat/completions`（也已在「模型 → 本地调试」预设中内置）
- 模型名称：`mock-chat`（或任意非空字符串）
- 演示聊天网页：`http://10.0.2.2:8787/demo`

## 权限清单

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 调用你自配的视觉模型接口 |
| `SYSTEM_ALERT_WINDOW` | 显示侧边悬浮球与 Compose 悬浮建议面板 |
| `PACKAGE_USAGE_STATS` | 通过 `UsageStatsManager` 检测前台应用包名 |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | 在离开侧答页面后维持悬浮球后台运行 |
| `POST_NOTIFICATIONS` | 展示「侧答正在运行」常驻通知（Android 13+） |
| **无 `BIND_ACCESSIBILITY_SERVICE`** | **完全不使用无障碍服务** |

## 项目结构

```text
app/src/main/java/app/sidereply/
├── MainActivity.kt                      Compose 入口、状态管理、Root 授权与连接测试调度
├── ai/
│   ├── AiClient.kt                      OpenAI 兼容 HttpURLConnection 客户端（含图片请求体）
│   ├── AiErrorMessages.kt               错误类型到中文提示的映射
│   ├── EndpointNormalizer.kt            Base URL / 完整路径补全与 HTTPS 校验
│   ├── Models.kt                        AiConfig / ReplySuggestion / AiException
│   ├── ProviderPresets.kt               主流视觉服务一键预设与匹配
│   ├── ReplyParser.kt                   JSON / Markdown 代码块 / 纯文本宽容解析
│   └── ReplyPrompts.kt                  「我」发送者视角锁定、自定义风格与换一组提示词
├── data/
│   ├── AppPreferences.kt                配置项持久化（主题模式、动态取色、显示范围、加密密钥）
│   └── SecretStore.kt                   Android Keystore AES-GCM 加解密
├── root/
│   ├── RootManager.kt                   appops 授权与权限就绪检查
│   ├── RootShell.kt                     su 命令执行与 stdout 内存截屏
│   └── WindowFocusProbe.kt              dumpsys window 焦点窗口解析（双开支持）
├── service/
│   └── FloatingAssistantService.kt      前台服务、UsageStats 轮询、M3 悬浮球与 Compose 悬浮窗调度
└── ui/
    ├── SheetRootLayout.kt               悬浮面板根容器（处理系统返回键）
    ├── icons/SideReplyIcons.kt          Material Symbols 风格矢量图标集
    ├── main/
    │   ├── MainUiState.kt               主界面标签与表单 UI 状态
    │   ├── SideReplyApp.kt              M3 Scaffold、LargeTopAppBar、NavigationBar 与弹窗
    │   ├── HomeTab.kt                   概览页（主控卡、Bento 状态卡、实景预览、上手指引）
    │   ├── ModelTab.kt                  模型页（快捷预设、表单校验、连接测试）
    │   └── SettingsTab.kt               偏好页（分组卡片、深浅主题、动态取色、隐私说明）
    ├── sheet/
    │   ├── OverlayLifecycleOwner.kt     WindowManager 悬浮窗内嵌 Compose 的生命周期宿主
    │   ├── ReplySheetUiState.kt         悬浮面板多轮对话与状态定义
    │   └── ReplySheetContent.kt         Compose M3 悬浮回复面板、快捷风格栏与交互动画
    └── theme/
        ├── Color.kt                     Material 3 浅色 / 深色 HCT 色板
        ├── Shape.kt                     M3 形状与分组圆角规范
        ├── Type.kt                      M3 排版层级
        └── Theme.kt                     SideReplyTheme（支持动态取色与深浅模式）
```

## 已知限制与注意事项

- **系统与权限限制**：仅支持 Android 11+，且依赖可用的 Root 管理器（如 Magisk / KernelSU）完成截屏；无 Root 环境下可正常浏览与配置界面，点击悬浮球截屏会提示找不到 `su`。
- **安全窗口不可截屏**：若当前聊天页面被系统或 ROM 标记为 `FLAG_SECURE` 安全窗口，系统截屏可能返回纯黑画面。
- **不自动发送、不保存记录**：侧答仅在点击「复制」后将文本写入剪贴板，由用户自行核对并手动粘贴发送；截屏和回复历史仅保存在内存中，关闭面板即清空。
- **不申请开机自启**：手机重启或强行停止应用后，需重新进入「侧答」启动悬浮球服务。
- **免责说明**：本项目为个人工具原型，与腾讯/微信或任何 Root 管理器无隶属关系；使用 Root 权限与第三方 AI 接口前请自行审阅源码并评估隐私风险。
