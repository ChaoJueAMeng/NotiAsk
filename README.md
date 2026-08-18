# NotiAsk（用户指南）

NotiAsk 让你直接在 **Android 通知栏输入框** 里向 AI 提问，不用切换到聊天应用。回复会以可展开通知返回，你可以在通知里继续追问。

## 这是什么

- 面向 Android 用户的通知栏问答工具（支持 Direct Reply）。
- 支持文字提问，也支持“截屏后提问”。
- API Key 仅保存在你的设备上，不上传到开发者服务器。

## 安装与使用

1. 安装 NotiAsk（Android 10 / API 29 及以上）。
2. 打开 App，添加并保存一组 AI 配置（服务商、Base URL、模型、API Key）。
3. 点击“启用通知栏问答”，按系统提示授权通知权限（Android 13+ 必需）。
4. 下拉通知栏并展开 NotiAsk 常驻通知：
   - 点“提问”：直接在通知输入框发问题。
   - 点“截屏搜索”：先截屏，再在通知里补充问题或直接提问。
5. 收到回复通知后可展开阅读，并点击“继续追问”发起下一轮问题。

## 服务商配置示例

| 服务商 | App 内选择 | Base URL | 模型示例 |
| --- | --- | --- | --- |
| OpenAI | OpenAI | `https://api.openai.com/v1` | `gpt-4.1-mini` |
| Anthropic | Anthropic | `https://api.anthropic.com` | `claude-sonnet-4-20250514` |
| 通义千问 | 通义千问 / DashScope | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| DeepSeek | DeepSeek | `https://api.deepseek.com` | `deepseek-v4-flash` |
| Kimi | Kimi / Moonshot | `https://api.moonshot.cn/v1` | `kimi-k2.6` |
| 其他兼容服务 | OpenAI 兼容接口 | 填该厂商兼容 API 根 URL | 填该厂商模型名 |

说明：
- Anthropic 使用原生 Messages 协议；
- 其他选项使用 OpenAI Chat Completions 兼容协议；
- Base URL 填“根地址”即可，App 会自动拼接接口路径。

## 隐私与安全

- API Key 会先用 Android Keystore 中不可导出的密钥加密，再保存到本地。
- 应用不会把你的 API Key 上传到自建后端（本项目无自建后端）。

## 常见问题

**1. 为什么看不到通知输入框？**  
请先确认已启用“通知栏问答”，并在 Android 13+ 授予通知权限。

**2. 截屏后为什么没有跳转到 App？**  
这是正常设计。截屏完成后会在通知栏提示“已截图”，你可直接在通知中提问。

**3. 可以连续对话吗？**  
当前是单轮问答模式；“继续追问”会作为新问题再次发送。

**4. 重启手机后为什么要重新启用？**  
为遵循系统后台与前台服务限制，重启后需要你再次进入 App 启用服务。
