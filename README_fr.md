<div align="center">

# MarkNote (墨记)

**Un éditeur Markdown élégant avec aperçu en temps réel pour Android** — édition
WYSIWYG façon Notion, aperçu façon Typora, fichiers locaux, synchronisation WebDAV
et 6 langues intégrées.

[English](README.md) · [中文](README_zh.md) · [**Français**](README_fr.md) ·
[Deutsch](README_de.md) · [日本語](README_ja.md) · [Español](README_es.md)

![Version](https://img.shields.io/badge/version-1.0.5-4a7bff)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin%20%2B%20Compose-orange)

</div>

---

## Qu'est-ce que MarkNote ?

MarkNote est un bloc-notes Markdown pensé pour le mobile. Au lieu d'afficher la syntaxe
Markdown brute pendant la saisie, il rend votre contenu en direct — titres, gras, listes,
images et blocs de code sont mis en forme instantanément, comme dans Notion ou Typora.

Vos notes vivent dans de **vrais fichiers `.md`** sur votre appareil, sans verrouillage.
Vous pouvez passer à un éditeur de code source, à une vue divisée côte à côte ou à un
aperçu en lecture seule à tout moment. La synchronisation WebDAV garde le même dossier
disponible sur vos autres appareils et serveurs.

## Captures d'écran

> 📷 Les captures sont maintenues par le propriétaire du projet. Placez vos images nommées
> `live.png`, `split.png`, `preview.png`, `formatting.png` et `webdav.png` dans
> [`docs/screenshots/`](docs/screenshots/) pour qu'elles apparaissent ci-dessous.
> Utilisez uniquement du contenu de démonstration — pas de notes réelles, d'adresses de
> serveur ou d'identifiants.

| Édition en direct | Vue divisée | Aperçu seul |
| --- | --- | --- |
| <img src="docs/screenshots/live.png" alt="Live" width="220" /> | <img src="docs/screenshots/split.png" alt="Split" width="220" /> | <img src="docs/screenshots/preview.png" alt="Preview" width="220" /> |

| Menu de mise en forme | Synchronisation WebDAV |
| --- | --- |
| <img src="docs/screenshots/formatting.png" alt="Formatting" width="220" /> | <img src="docs/screenshots/webdav.png" alt="WebDAV" width="220" /> |

## Fonctionnalités

- **Édition en direct façon Notion** — tapez `/` pour insérer titres, gras, listes,
  citations, tableaux, images et plus ; les marqueurs Markdown sont masqués et rendus
  en temps réel.
- **Aperçu façon Typora** — vue divisée et aperçu seul rendus nativement, images locales
  affichées en ligne et automatiquement disposées en blocs.
- **Fichiers locaux d'abord** — les notes sont stockées en vrais fichiers `.md` dans le
  dossier de documents de l'application ; les images sont copiées dans `Images/` et
  référencées par chemins relatifs.
- **Synchronisation WebDAV bidirectionnelle** — synchronisation sûre (aucune suppression
  accidentelle), synchronisation automatique au lancement et affichage/masquage du mot
  de passe.
- **6 langues** — 简体中文, English, Français, Deutsch, 日本語, Español, y compris
  l'interface du moteur d'édition, le menu `/` et les espaces réservés.
- **Trois modes d'édition** — WYSIWYG en direct, éditeur source avec coloration syntaxique,
  et vue divisée/aperçu, en conservant le curseur et l'historique d'annulation.
- **Barre d'outils compacte et élégante** — barre fixe qui suit le clavier, avec un
  sélecteur de titres et un sélecteur de listes à puces/numérotées.

## Téléchargement

Le dernier APK est publié avec chaque version :

- [**MarkNote-1.0.5.apk**](releases/MarkNote-1.0.5.apk) (également joint à la
  [GitHub Release](https://github.com/Ninewansen/MarkNote/releases))

Installez-le directement sur Android 8.0+ (API 26+). Aucun service Google Play requis.

## Compiler depuis les sources

Prérequis :

- Android Studio (ou SDK Android + JDK 17)
- Android SDK Platform 36

```bash
git clone git@github.com:Ninewansen/MarkNote.git
cd MarkNote
./gradlew :app:assembleDebug
```

La signature de la version release est lue depuis `keystore.properties` à la racine du
projet (**non commité**). Pour un APK release signé, créez ce fichier localement :

```properties
storeFile=keystore/marknote.keystore
storePassword=votre-mot-de-passe
keyAlias=votre-alias
keyPassword=votre-clé
```

Sans lui, `assembleRelease` produit un APK non signé. Ne commitez jamais votre keystore.

## Synchronisation WebDAV

1. Ouvrez le menu de l'application → **Synchronisation WebDAV**.
2. Renseignez l'**URL du serveur** (adresse `https://…` complète), le **nom
   d'utilisateur** et le **mot de passe**.
3. Touchez **Synchroniser maintenant** (ou activez **Synchronisation automatique au
   lancement**).

La synchronisation est sûre par conception : les fichiers manquants sont copiés, les
fichiers modifiés sont envoyés/téléchargés, et rien n'est jamais supprimé automatiquement.

## Localisation

| Langue | Code | README | Statut |
| --- | --- | --- | --- |
| 简体中文 | `zh` | [README_zh.md](README_zh.md) | ✅ |
| English | `en` | [README.md](README.md) | ✅ |
| Français | `fr` | [README_fr.md](README_fr.md) | ✅ |
| Deutsch | `de` | [README_de.md](README_de.md) | ✅ |
| 日本語 | `ja` | [README_ja.md](README_ja.md) | ✅ |
| Español | `es` | [README_es.md](README_es.md) | ✅ |

## Technologies

- **Kotlin + Jetpack Compose (Material 3)** — interface
- [**Vditor**](https://github.com/Vanessa219/vditor) — moteur d'édition WYSIWYG / rendu en direct
- [**Markwon**](https://github.com/noties/Markwon) — rendu natif Spannable pour l'aperçu
- [**Sora Editor**](https://github.com/Rosemoe/sora-editor) — éditeur de code source avec
  coloration syntaxique
- **OkHttp** — client WebDAV

## Confidentialité

- Toutes les notes et images sont stockées **localement sur votre appareil**.
- Les identifiants WebDAV sont conservés dans les préférences privées de l'application et
  envoyés uniquement au serveur que vous configurez. Utilisez HTTPS.
- Aucune statistique, aucun suivi, aucun appel réseau sauf si vous synchronisez.

## Licence

Publié sous [licence MIT](LICENSE).

## Remerciements

Merci aux projets open source qui rendent MarkNote possible : Vditor, Markwon,
Sora Editor, Prism4j et OkHttp. Le design des interactions s'inspire de Notion et Typora.
