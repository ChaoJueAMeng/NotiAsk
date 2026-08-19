<div align="center">

# NotiAsk

### Ask AI directly from your Android notification shade

**No chat app to open. Pull down → ask → read the answer in a notification.**

[简体中文](README.md) · [Features](#-features) · [Quick start](#-quick-start) · [Provider setup](#-provider-setup) · [Privacy & security](#-privacy--security)

</div>

> AI does not always need to live inside an app. NotiAsk puts the asking interface in the Android notification shade you already use every day.

<!--
  Before publishing, add a 10–20 second demo GIF here:
  pull down → ask → read the reply → screenshot ask.
  Example: ![NotiAsk demo](docs/demo.gif)
-->

## 🎯 Why choose NotiAsk?

The usual AI workflow is:

> **Using another app → think of a question → switch apps or open a floating window to ask AI → wait for the answer… → switch back**

**NotiAsk shortens it to:**

> **Using another app → think of a question → pull down the notification shade to ask AI → continue what you were doing → check the answer in the shade whenever you want**

**NotiAsk gives you:**

>- **No need to leave the app you are using or open split-screen / a floating window. Pull down to ask, or capture the screen and ask in one tap.**
>- **While composing a question, swipe up at any time to quickly check what you are asking about or reply to an important message; NotiAsk keeps your draft.**
>- **After you ask, the AI generates in the background while you continue what you were doing.**
>- **When the answer is ready, you receive a notification that you can expand, read, and copy at any time.**

### NotiAsk is not trying to be another chat client. It makes AI available like a system feature when you need it: fewer interface switches and fewer interruptions.

## ✨ Features

- **Ask from a notification** — Uses Android Direct Reply, so you can type a question in the persistent notification.
- **Read answers in the shade** — Replies arrive as expandable notifications and can be copied with one tap.
- **Ask about a screenshot** — Start a screenshot from the notification, add a question when it is captured, or let a vision-capable model explain the screen directly.
- **Switch models in the notification** — Change among saved profiles without opening the app. The active profile is pinned first; longer lists are paginated.
- **Multiple providers and profiles** — Save, name, and switch between several API configurations.
- **Direct-to-provider requests** — NotiAsk has no proxy backend; requests go from your device to the AI provider you configure.
- **Encrypted key storage** — API keys are encrypted locally with a non-exportable Android Keystore key.

## 📱 How it works

```text
Add an AI profile and enable notifications
                 ↓
Pull down and expand the NotiAsk notification
                 ↓
Ask / switch model / screenshot ask
                 ↓
Read and copy the answer in the notification shade
```

NotiAsk uses **single-turn questions**. Each request is independent and does not automatically include previous questions or answers.

## 🚀 Quick start

### Requirements

- Android 10 (API 29) or later
- An API key for your chosen AI provider
- Notification permission on Android 13 and later

### Configure and ask

1. Install and open NotiAsk, then select **Add**.
2. Choose a provider and enter its API key, base URL, and model name. You can also give the profile a friendly name.
3. Enable **Use this model after saving**, save the profile, then select **Enable notification Q&A**.
4. Grant the notification permission when Android asks.
5. Pull down and expand the persistent NotiAsk notification:
   - select **Ask** and enter a question in the notification input;
   - select **Switch model** to choose a saved profile;
   - select **Screenshot ask**, approve the system capture prompt, then add a question in the *Screenshot captured* notification or select **Ask directly**.
6. Expand the answer notification to read it, and select **Copy** to put the answer on the clipboard.

> Some Android builds are aggressive about background processes. If the persistent notification is unreliable, use the app's **Battery optimization** shortcut to allow NotiAsk to keep running.

## 🤖 Provider setup

| Provider | In-app option | Base URL | Protocol |
| --- | --- | --- | --- |
| OpenAI | OpenAI | `https://api.openai.com/v1` | Chat Completions-compatible |
| Anthropic | Anthropic | `https://api.anthropic.com` | Native Messages |
| Qwen | Qwen / DashScope | `https://dashscope.aliyuncs.com/compatible-mode/v1` | Chat Completions-compatible |
| DeepSeek | DeepSeek | `https://api.deepseek.com` | Chat Completions-compatible |
| Kimi | Kimi / Moonshot | `https://api.moonshot.cn/v1` | Chat Completions-compatible |
| Other services | OpenAI-compatible API | The provider's API root URL | Chat Completions-compatible |

- Enter the API **root URL** as the base URL; NotiAsk adds the endpoint path.
- Check the provider's current documentation for the exact model name.
- Screenshot ask requires both the selected model and its API endpoint to accept image input. The app shows an error if the provider does not support it.

## 🔐 Privacy & security

- API keys are encrypted with a non-exportable AES-GCM key in Android Keystore before being written to app-private storage.
- NotiAsk does not operate a backend API proxy. Your questions, screenshots, and API key are not sent to a developer-operated server.
- Questions and any screenshot you choose to use are sent to the AI provider you configure. Please review that provider's privacy and data-processing terms.
- A captured screenshot remains in memory only until the current question consumes it or it is discarded; it is not saved as a media-library file by the app.

## ❓ FAQ

**Why is there no notification input?** Make sure a usable profile exists and **notification Q&A** is enabled. On Android 13+, notification permission must also be granted.

**Does it support ongoing conversations?** Not currently. Every question is a single, independent request.

**What if I dismiss the persistent notification?** The app tries to restore it. If it does not return, open NotiAsk and check the service state and notification permission.

**Why must I enable it again after a reboot?** To respect Android background and foreground-service restrictions, the service must be manually enabled again after the device restarts.

## 🤝 Contributing

Issues, feature ideas, and pull requests are welcome. Real-device notification compatibility feedback and tested configurations for more OpenAI-compatible providers are especially useful.

If NotiAsk is useful to you, a Star helps more people discover a lighter way to use AI.

## 📄 License

NotiAsk is licensed under the MIT License.
