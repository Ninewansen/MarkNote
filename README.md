<div align="center">

# MarkNote (墨记)

**A polished, real-time Markdown editor for Android** — Notion-style WYSIWYG editing,
Typora-like live preview, local-first file storage, WebDAV sync, and 6 built-in languages.

[**English**](README.md) · [中文](README_zh.md) · [Français](README_fr.md) ·
[Deutsch](README_de.md) · [日本語](README_ja.md) · [Español](README_es.md)

![Version](https://img.shields.io/badge/version-1.0.5-4a7bff)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin%20%2B%20Compose-orange)

</div>

---

## What is MarkNote?

MarkNote is a mobile-first Markdown notebook. Instead of showing raw Markdown syntax while
you type, it renders your content live — headings, bold, lists, images and code blocks are
formatted instantly as you edit, just like Notion or Typora.

Your notes live in **files on your device** (plain `.md` files, no lock-in). You can switch
to a traditional source editor, a side-by-side split view, or a clean read-only preview at
any time. WebDAV sync keeps the same folder available on your other devices and servers.

## Screenshots

> 📷 Screenshots are maintained by the project owner. Put your own images named
> `live.png`, `split.png`, `preview.png`, `formatting.png` and `webdav.png` into
> [`docs/screenshots/`](docs/screenshots/) and the table below will render them.
> Please use demo content only — no real notes, server addresses or credentials.

| Live editing | Split view | Preview only |
| --- | --- | --- |
| ![Live](docs/screenshots/live.png) | ![Split](docs/screenshots/split.png) | ![Preview](docs/screenshots/preview.png) |

| Formatting menu | WebDAV sync |
| --- | --- |
| ![Formatting](docs/screenshots/formatting.png) | ![WebDAV](docs/screenshots/webdav.png) |

## Features

- **Notion-style live editing** — type `/` to insert headings, bold, lists, quotes, tables,
  images and more; Markdown markers are hidden and rendered in real time.
- **Typora-like preview** — split view and preview-only modes rendered natively, with local
  images displayed inline and automatically laid out as blocks.
- **Local-first files** — notes are stored as real `.md` files in the app's document folder;
  images are copied into `Images/` and referenced with relative paths.
- **WebDAV two-way sync** — safe bidirectional sync (no accidental deletion), with
  auto-sync on launch and a password visibility toggle.
- **6 languages** — 简体中文, English, Français, Deutsch, 日本語, Español, including the
  editor kernel UI, slash menu and placeholders.
- **Three editing modes** — live WYSIWYG, source editor with syntax highlighting, and
  split/preview, all preserving cursor position and undo history.
- **Beautiful, compact toolbar** — a fixed bottom bar that follows the keyboard, with a
  heading picker and a bullet/ordered list picker.

## Download

The latest APK is published with each release:

- [**MarkNote-1.0.5.apk**](releases/MarkNote-1.0.5.apk) (also attached to the
  [GitHub Release](https://github.com/Ninewansen/MarkNote/releases))

Install it directly on Android 8.0+ (API 26+). No Google Play services required.

## Build from source

Requirements:

- Android Studio (or Android SDK + JDK 17)
- Android SDK Platform 36

```bash
git clone git@github.com:Ninewansen/MarkNote.git
cd MarkNote
./gradlew :app:assembleDebug
```

The release build reads signing credentials from `keystore.properties` in the project root
(**not committed**). For a signed release APK, create that file locally:

```properties
storeFile=keystore/marknote.keystore
storePassword=your-store-password
keyAlias=your-alias
keyPassword=your-key-password
```

Without it, `assembleRelease` produces an unsigned APK. Never commit your keystore.

## WebDAV sync

1. Open the app menu → **WebDAV sync**.
2. Fill in the **Server URL** (a full `https://…` address), **Username** and **Password**.
3. Tap **Sync now** (or enable **Auto-sync on launch**).

The sync is safe by design: files missing on either side are copied, changed files are
uploaded/downloaded, and nothing is ever deleted automatically.

## Localization

| Language | Code | README | Status |
| --- | --- | --- | --- |
| 简体中文 | `zh` | [README_zh.md](README_zh.md) | ✅ |
| English | `en` | [README.md](README.md) | ✅ |
| Français | `fr` | [README_fr.md](README_fr.md) | ✅ |
| Deutsch | `de` | [README_de.md](README_de.md) | ✅ |
| 日本語 | `ja` | [README_ja.md](README_ja.md) | ✅ |
| Español | `es` | [README_es.md](README_es.md) | ✅ |

## Tech stack

- **Kotlin + Jetpack Compose (Material 3)** — UI
- [**Vditor**](https://github.com/Vanessa219/vditor) — WYSIWYG / live rendering kernel
- [**Markwon**](https://github.com/noties/Markwon) — native Spannable preview rendering
- [**Sora Editor**](https://github.com/Rosemoe/sora-editor) — source code editor with
  syntax highlighting
- **OkHttp** — WebDAV client

## Privacy

- All notes and images are stored **locally on your device**.
- WebDAV credentials are stored in private app preferences and only sent to the server you
  configure. Use HTTPS.
- No analytics, no tracking, no network calls unless you sync.

## License

Released under the [MIT License](LICENSE).

## Acknowledgements

Thanks to the open-source projects that make MarkNote possible: Vditor, Markwon,
Sora Editor, Prism4j and OkHttp. The interaction design is inspired by Notion and Typora.
