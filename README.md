# NotiAsk

通过 Android 系统通知栏的 Direct Reply 输入框直接向 AI 提问，回答会以可展开通知返回。项目是纯 Kotlin + Jetpack Compose 客户端，不经过自建后端。

## 快速开始

1. 用 Android Studio 打开本目录并完成 Gradle Sync（JDK 17+、Android SDK Platform 36）。首次同步会下载 Gradle/Android 依赖。
2. 安装到 Android 10（API 29）或更高版本的设备。
3. 打开 App，添加一组 AI 配置并保存；API Key 只保存在设备上。
4. 点击“启用通知栏问答”，在 Android 13+ 授予通知权限。
5. 下拉并展开 NotiAsk 的常驻通知，点“提问”输入问题；回答通知可展开阅读并继续追问。

## 服务商配置

| 服务商 | 服务商选择 | Base URL | 模型示例 |
| --- | --- | --- | --- |
| OpenAI | OpenAI | `https://api.openai.com/v1` | `gpt-4.1-mini` |
| Anthropic | Anthropic | `https://api.anthropic.com` | `claude-sonnet-4-20250514` |
| 通义千问 | 通义千问 / DashScope | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| DeepSeek | DeepSeek | `https://api.deepseek.com` | `deepseek-v4-flash`（也可填 `deepseek-v4-pro`） |
| Kimi | Kimi / Moonshot | `https://api.moonshot.cn/v1` | `kimi-k2.6`（也可填 `kimi-k3`、`kimi-k2`） |
| 智谱等其他兼容服务 | OpenAI 兼容接口 | 填该厂商的兼容 API 根 URL | 填该厂商模型名 |

Anthropic 使用原生 Messages 协议及 `x-api-key`；其他选择均走 OpenAI Chat Completions 兼容协议。Base URL 是根 URL，应用会自动追加 `chat/completions` 或 `v1/messages`。

## 项目结构

```
app/src/main/java/com/notiask/
  ai/             # AiServiceAdapter、OpenAI 兼容与 Anthropic 协议实现
  data/           # 多配置仓库、Android Keystore AES-GCM 加密
  notification/   # 通知渠道、RemoteInput、广播接收器、前台服务
  MainActivity.kt # Compose 设置页
```

## 安全与平台说明

- API Key 使用 Android Keystore 中不可导出的 AES-GCM 密钥加密后才写入 SharedPreferences；配置元数据不含 Key。应用禁用 Auto Backup，避免密文与设备密钥分离。
- `EncryptedSharedPreferences` 已被 AndroidX 废弃，故采用官方建议的 Android Keystore + 标准 SharedPreferences 组合。
- 常驻通知服务声明 `specialUse`，避免 Android 15+ 对 `dataSync` 前台服务的每日 6 小时限制。发布 Google Play 时，需要在 Play Console 如实说明此特定前台服务用途并接受审核。
- 部分小米、华为、OPPO、vivo 等系统会限制后台运行。App 仅引导用户前往官方电池优化设置，不使用绕过系统限制的保活方式。
- Android 13+ 拒绝通知权限时，前台服务通知只会在系统任务管理器显示，无法提供通知栏输入框。

## 已知限制

- 当前为无上下文的单轮问答；“继续追问”只是再次发送新的单轮问题。
- 网络请求的连接超时为 20 秒、整体调用上限为 100 秒。错误会以通知明确提示。
- 设备重启后需要用户再次打开 App 并启用服务；这避免了在系统后台限制下不合规地启动前台服务。
