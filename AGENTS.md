# NotiAsk

NotiAsk 是纯 Kotlin + Jetpack Compose 的 Android 客户端，通过系统通知栏的 Direct Reply 输入框向 AI 提问。项目概览、服务商配置与安全说明见 `README.md`。

## Cursor Cloud specific instructions

面向后续 Cloud Agent 的持久化说明（基础镜像已通过 setup 安装好 JDK 21、Android SDK 与 Gradle，启动时会自动执行 update 脚本，无需再手动装依赖）。

### 工具链与路径

- JDK 21、Android SDK 位于 `$HOME/android-sdk`（Platform 36、build-tools 36.0.0、platform-tools、emulator）。`ANDROID_HOME` / `PATH` 已写入 `~/.bashrc`。
- Gradle 9.7.0 安装在 `$HOME/gradle-9.7.0`，并软链为全局命令 `gradle`。
- **仓库缺少 Gradle wrapper（没有 `gradlew` 与 `gradle-wrapper.jar`），请直接使用系统命令 `gradle`，不要用 `./gradlew`。**
- `local.properties`（`sdk.dir`）被 `.gitignore` 忽略，由 update 脚本在每次启动时重建；不要提交它。

### 构建 / 测试 / Lint

- 构建 Debug APK：`gradle :app:assembleDebug`（产物在 `app/build/outputs/apk/debug/app-debug.apk`）。
- 单元测试：`gradle :app:testDebugUnitTest`。
- Lint：`gradle :app:lintDebug`。注意：当前存在 **1 个既有的** `MissingPermission` 报错（`NotificationController.kt` 调用 `notify`），会让该 task 以非零码结束，这是仓库代码本身的问题、并非环境问题；排查环境时不要因此误判。

### 运行 App（模拟器）

- **本嵌套 VM 中 KVM 硬件加速无法真正运行 guest**（`emulator -accel-check` 会显示可用，但带 `-accel on` 时 guest vCPU 卡死、无内核输出）。必须用纯软件模式启动：`-accel off -gpu off`。
- 已创建可用 AVD `notiask34`（`system-images;android-34;default;x86_64`，AOSP 镜像启动更稳）。App `minSdk=29`，在 API 34 上运行没问题。启动示例（headless）：
  `emulator -avd notiask34 -no-window -no-audio -no-snapshot -accel off -gpu off -no-boot-anim`
- 软件模式下**冷启动约 4 分钟**；刚开机 1~2 分钟内 system/systemui 会弹瞬时 ANR（"isn't responding"），点 Wait 或等待即可，**不要 kill**。
- 模拟器为 headless，用 `adb` 交互：`adb exec-out screencap -p > x.png` 截图、`adb shell uiautomator dump` 取控件坐标、`adb shell input tap/text` 操作。
- Compose 输入框注意：`adb shell input text` 只有在字段获得焦点、IME 连接就绪后才生效（软件模式慢，点完字段先 sleep 几秒再输入）；`ESC`/`BACK` 键会关闭 Compose 的 `AlertDialog`，需要收起键盘时用 `Enter`（`keyevent 66`），不要用 ESC。
- 冒烟验证：启动 App → 点"添加"填 API Key 保存（密钥经 Android Keystore AES-GCM 加密后写入 `shared_prefs/notiask_config.xml`，可用 `adb shell run-as com.notiask cat ...` 确认非明文）→ 点"启用通知栏问答"授予通知权限，前台服务 `QuestionService` 会常驻并推送带输入框的通知。
