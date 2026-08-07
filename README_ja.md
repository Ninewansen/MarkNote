<div align="center">

# MarkNote

**Android 向けの洗練されたリアルタイム Markdown エディタ** —— Notion 風 WYSIWYG 編集、
Typora 風ライブプレビュー、ローカルファイル優先、WebDAV 同期、6 言語対応。

[English](README.md) · [中文](README_zh.md) · [Français](README_fr.md) ·
[Deutsch](README_de.md) · [**日本語**](README_ja.md) · [Español](README_es.md)

![Version](https://img.shields.io/badge/version-1.0.5-4a7bff)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin%20%2B%20Compose-orange)

</div>

---

## MarkNote とは？

MarkNote はモバイルファーストの Markdown ノートアプリです。入力中に生の Markdown 記法を
表示するのではなく、見出し・太字・リスト・画像・コードブロックを**リアルタイムに整形**して
表示します。Notion や Typora と同じ感覚です。

ノートは端末上の**実際の `.md` ファイル**として保存され、特定サービスへの依存はありません。
いつでもソースエディタ、左右分割ビュー、読み取り専用プレビューに切り替えられます。
WebDAV 同期を使えば、同じフォルダを他の端末やサーバーと共有できます。

## スクリーンショット

> 📷 スクリーンショットはプロジェクトオーナーが管理します。`live.png`、`split.png`、
> `preview.png`、`formatting.png`、`webdav.png` という名前の画像を
> [`docs/screenshots/`](docs/screenshots/) に置くと、下の表に表示されます。
> デモ用の内容のみを使用してください（実在のノート・サーバーアドレス・認証情報は不可）。

| ライブ編集 | 分割表示 | プレビューのみ |
| --- | --- | --- |
| <img src="docs/screenshots/live.png" alt="Live" width="220" /> | <img src="docs/screenshots/split.png" alt="Split" width="220" /> | <img src="docs/screenshots/preview.png" alt="Preview" width="220" /> |

| 書式メニュー | WebDAV 同期 |
| --- | --- |
| <img src="docs/screenshots/formatting.png" alt="Formatting" width="220" /> | <img src="docs/screenshots/webdav.png" alt="WebDAV" width="220" /> |

## 機能

- **Notion 風ライブ編集** —— `/` を入力して見出し・太字・リスト・引用・表・画像などを挿入。
  Markdown 記号は自動的に隠れてリアルタイムに描画されます。
- **Typora 風プレビュー** —— 分割表示とプレビューのみモードをネイティブ描画。ローカル画像は
  インライン表示され、自動的にブロックとして配置されます。
- **ローカルファイル優先** —— ノートはアプリのドキュメントフォルダに実 `.md` ファイルとして
  保存。画像は `Images/` にコピーされ、相対パスで参照されます。
- **WebDAV 双方向同期** —— 誤削除のない安全な同期、起動時自動同期、パスワード表示切替に対応。
- **6 言語** —— 简体中文・English・Français・Deutsch・日本語・Español。エディタ本体、
  スラッシュメニュー、プレースホルダーもすべてローカライズ。
- **3 つの編集モード** —— ライブ WYSIWYG、シンタックスハイライト付きソースエディタ、
  分割/プレビュー。カーソル位置と Undo 履歴を保持します。
- **美しいコンパクトなツールバー** —— キーボードに追従する固定バー。見出しピッカーと
  箇条書き/番号付きリストピッカーを搭載。

## ダウンロード

最新の APK は各リリースで公開しています：

- [**MarkNote-1.0.5.apk**](releases/MarkNote-1.0.5.apk)（
  [GitHub Release](https://github.com/Ninewansen/MarkNote/releases) にも添付）

Android 8.0+（API 26+）に直接インストールできます。Google Play サービスは不要です。

## ソースからビルド

必要なもの：

- Android Studio（または Android SDK + JDK 17）
- Android SDK Platform 36

```bash
git clone git@github.com:Ninewansen/MarkNote.git
cd MarkNote
./gradlew :app:assembleDebug
```

リリース署名はプロジェクト直下の `keystore.properties` から読み込みます
（**リポジトリにはコミットしません**）。署名付き APK をビルドするには、ローカルに作成：

```properties
storeFile=keystore/marknote.keystore
storePassword=あなたのストアパスワード
keyAlias=あなたのエイリアス
keyPassword=あなたのキーパスワード
```

このファイルがない場合、`assembleRelease` は未署名の APK を生成します。keystore は絶対に
コミットしないでください。

## WebDAV 同期

1. アプリメニュー → **WebDAV 同期** を開きます。
2. **サーバー URL**（完全な `https://…` アドレス）、**ユーザー名**、**パスワード**を入力。
3. **今すぐ同期** をタップ（または **起動時に自動同期** を有効化）。

同期は安全設計です。不足ファイルはコピーされ、変更されたファイルはアップロード/ダウンロード
されます。どちらかのファイルが自動削除されることはありません。

## 対応言語

| 言語 | コード | README | 状態 |
| --- | --- | --- | --- |
| 简体中文 | `zh` | [README_zh.md](README_zh.md) | ✅ |
| English | `en` | [README.md](README.md) | ✅ |
| Français | `fr` | [README_fr.md](README_fr.md) | ✅ |
| Deutsch | `de` | [README_de.md](README_de.md) | ✅ |
| 日本語 | `ja` | [README_ja.md](README_ja.md) | ✅ |
| Español | `es` | [README_es.md](README_es.md) | ✅ |

## 技術スタック

- **Kotlin + Jetpack Compose（Material 3）** —— UI
- [**Vditor**](https://github.com/Vanessa219/vditor) —— WYSIWYG / ライブ描画エンジン
- [**Markwon**](https://github.com/noties/Markwon) —— ネイティブ Spannable プレビュー描画
- [**Sora Editor**](https://github.com/Rosemoe/sora-editor) —— シンタックスハイライト付き
  ソースエディタ
- **OkHttp** —— WebDAV クライアント

## プライバシー

- すべてのノートと画像は**端末ローカルにのみ**保存されます。
- WebDAV の認証情報はアプリのプライベート設定に保存され、設定したサーバーにのみ送信されます。
  HTTPS を使用してください。
- 広告・統計・トラッキングなし。同期しない限りネットワーク通信もしません。

## ライセンス

[MIT License](LICENSE) で公開しています。

## 謝辞

MarkNote を可能にしてくれたオープンソースプロジェクトに感謝します：Vditor、Markwon、
Sora Editor、Prism4j、OkHttp。インタラクションは Notion と Typora に着想を得ています。
