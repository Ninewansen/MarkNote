<div align="center">

# MarkNote（墨记）

**一款精致、实时预览的 Android Markdown 编辑器** —— Notion 式所见即所得编辑、Typora 式实时预览、
本地文件优先、支持 WebDAV 同步和 6 种内置语言。

[English](README.md) · [**中文**](README_zh.md) · [Français](README_fr.md) ·
[Deutsch](README_de.md) · [日本語](README_ja.md) · [Español](README_es.md)

![Version](https://img.shields.io/badge/version-1.0.5-4a7bff)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin%20%2B%20Compose-orange)

</div>

---

## 这是什么？

MarkNote 是一款移动端优先的 Markdown 笔记应用。输入时不会看到一堆 `**`、`#` 之类的语法标记，
而是像 Notion / Typora 一样**实时渲染**：标题、加粗、列表、图片、代码块在你输入的同时就变成最终效果。

笔记以**真实的 `.md` 文件**保存在设备上，随时可以切回源码编辑、分屏对照或纯净预览模式。
配合 WebDAV 双向同步，同一份笔记文件夹可以同步到你的服务器和其他设备。

## 截图

> 📷 截图由项目维护者自行维护。请把命名为 `live.png`、`split.png`、`preview.png`、
> `formatting.png`、`webdav.png` 的图片放进 [`docs/screenshots/`](docs/screenshots/)，
> 下面的表格会自动显示。请只使用演示内容——不要出现真实笔记、服务器地址或账号密码。

| 实时编辑 | 分屏 | 仅预览 |
| --- | --- | --- |
| <img src="docs/screenshots/live.png" alt="Live" width="220" /> | <img src="docs/screenshots/split.png" alt="Split" width="220" /> | <img src="docs/screenshots/preview.png" alt="Preview" width="220" /> |

| 格式菜单 | WebDAV 同步 |
| --- | --- |
| <img src="docs/screenshots/formatting.png" alt="Formatting" width="220" /> | <img src="docs/screenshots/webdav.png" alt="WebDAV" width="220" /> |

## 功能特性

- **Notion 式实时编辑**：输入 `/` 唤起标题、加粗、列表、引用、表格、图片等命令，Markdown
  标记即时隐藏并渲染。
- **Typora 式预览**：分屏与仅预览模式采用原生渲染，本地图片自动按块级排版，不重叠、不乱码。
- **本地文件优先**：笔记以真实 `.md` 文件保存在应用文档目录，图片复制到 `Images/` 并用相对路径引用。
- **WebDAV 双向同步**：安全同步、不误删任何一侧文件，支持启动自动同步和密码显示/隐藏。
- **6 种语言**：简体中文、English、Français、Deutsch、日本語、Español，编辑器内核、
  斜杠菜单和占位符全部本地化。
- **三种编辑模式**：实时所见即所得、带语法高亮的源码编辑、分屏/预览，切换不丢光标与撤销记录。
- **美观紧凑的工具栏**：固定底栏跟随键盘上移，标题和列表按钮支持展开选择。

## 下载

最新 APK 随每个版本发布：

- [**MarkNote-1.0.5.apk**](releases/MarkNote-1.0.5.apk)（同时附加在
  [GitHub Release](https://github.com/Ninewansen/MarkNote/releases) 中）

支持 Android 8.0+（API 26+），无需 Google Play 服务，直接安装。

## 从源码构建

环境要求：

- Android Studio（或 Android SDK + JDK 17）
- Android SDK Platform 36

```bash
git clone git@github.com:Ninewansen/MarkNote.git
cd MarkNote
./gradlew :app:assembleDebug
```

Release 签名信息从项目根目录的 `keystore.properties` 读取（**不会提交到仓库**）。
要生成签名版 APK，请在本地创建该文件：

```properties
storeFile=keystore/marknote.keystore
storePassword=你的存储密码
keyAlias=你的别名
keyPassword=你的密钥密码
```

没有该文件时 `assembleRelease` 会生成未签名 APK。请勿把 keystore 提交到仓库。

## WebDAV 同步

1. 打开应用菜单 → **WebDAV 同步**。
2. 填写 **服务器地址**（完整的 `https://…` 地址）、**用户名**和**密码**。
3. 点击 **立即同步**（或开启 **启动时自动同步**）。

同步是安全设计：缺少的文件会互相补齐，有变更的文件会上传/下载，任何一侧的文件都不会被自动删除。

## 多语言

| 语言 | 代码 | README | 状态 |
| --- | --- | --- | --- |
| 简体中文 | `zh` | [README_zh.md](README_zh.md) | ✅ |
| English | `en` | [README.md](README.md) | ✅ |
| Français | `fr` | [README_fr.md](README_fr.md) | ✅ |
| Deutsch | `de` | [README_de.md](README_de.md) | ✅ |
| 日本語 | `ja` | [README_ja.md](README_ja.md) | ✅ |
| Español | `es` | [README_es.md](README_es.md) | ✅ |

## 技术栈

- **Kotlin + Jetpack Compose（Material 3）** —— 界面
- [**Vditor**](https://github.com/Vanessa219/vditor) —— 所见即所得 / 实时渲染内核
- [**Markwon**](https://github.com/noties/Markwon) —— 原生 Spannable 预览渲染
- [**Sora Editor**](https://github.com/Rosemoe/sora-editor) —— 带语法高亮的源码编辑器
- **OkHttp** —— WebDAV 客户端

## 隐私

- 所有笔记和图片**只保存在设备本地**。
- WebDAV 凭据保存在应用私有配置中，只会发送给你配置的服务器。请使用 HTTPS。
- 无广告、无统计、无追踪；不同步就不联网。

## 开源协议

基于 [MIT License](LICENSE) 发布。

## 致谢

感谢让 MarkNote 成为可能的开源项目：Vditor、Markwon、Sora Editor、Prism4j 和 OkHttp。
交互设计参考了 Notion 与 Typora。
