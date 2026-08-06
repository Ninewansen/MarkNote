<div align="center">

# MarkNote (墨记)

**Un editor Markdown elegante con vista previa en tiempo real para Android** — edición
WYSIWYG estilo Notion, vista previa estilo Typora, archivos locales, sincronización
WebDAV y 6 idiomas integrados.

[English](README.md) · [中文](README_zh.md) · [Français](README_fr.md) ·
[Deutsch](README_de.md) · [日本語](README_ja.md) · [**Español**](README_es.md)

![Version](https://img.shields.io/badge/version-1.0.5-4a7bff)
![License](https://img.shields.io/badge/license-MIT-green)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin%20%2B%20Compose-orange)

</div>

---

## ¿Qué es MarkNote?

MarkNote es un bloc de notas Markdown pensado para el móvil. En lugar de mostrar la sintaxis
Markdown mientras escribes, renderiza el contenido en vivo — encabezados, negrita, listas,
imágenes y bloques de código se formatean al instante, como en Notion o Typora.

Tus notas viven en **archivos `.md` reales en tu dispositivo**, sin depender de ningún
servicio. Puedes cambiar a un editor de código fuente, a una vista dividida o a una vista
previa de solo lectura en cualquier momento. La sincronización WebDAV mantiene la misma
carpeta disponible en tus otros dispositivos y servidores.

## Capturas de pantalla

> 📷 Las capturas las mantiene el propietario del proyecto. Coloca tus imágenes con los
> nombres `live.png`, `split.png`, `preview.png`, `formatting.png` y `webdav.png` en
> [`docs/screenshots/`](docs/screenshots/) para que aparezcan en la tabla siguiente.
> Usa solo contenido de demostración — nada de notas reales, direcciones de servidor o credenciales.

| Edición en vivo | Vista dividida | Solo vista previa |
| --- | --- | --- |
| ![Live](docs/screenshots/live.png) | ![Split](docs/screenshots/split.png) | ![Preview](docs/screenshots/preview.png) |

| Menú de formato | Sincronización WebDAV |
| --- | --- |
| ![Formatting](docs/screenshots/formatting.png) | ![WebDAV](docs/screenshots/webdav.png) |

## Características

- **Edición en vivo estilo Notion** — escribe `/` para insertar encabezados, negrita,
  listas, citas, tablas, imágenes y más; los marcadores Markdown se ocultan y se renderizan
  en tiempo real.
- **Vista previa estilo Typora** — vista dividida y modo de solo vista previa renderizados
  de forma nativa, con imágenes locales mostradas en línea y dispuestas automáticamente como bloques.
- **Archivos locales primero** — las notas se guardan como archivos `.md` reales en la
  carpeta de documentos de la app; las imágenes se copian en `Images/` y se referencian con
  rutas relativas.
- **Sincronización WebDAV bidireccional** — sincronización segura (sin borrados
  accidentales), sincronización automática al iniciar y botón para mostrar/ocultar la contraseña.
- **6 idiomas** — 简体中文, English, Français, Deutsch, 日本語, Español, incluida la interfaz
  del editor, el menú de barra diagonal y los marcadores de posición.
- **Tres modos de edición** — WYSIWYG en vivo, editor de código fuente con resaltado de
  sintaxis y vista dividida/vista previa, conservando el cursor y el historial de deshacer.
- **Barra de herramientas compacta y elegante** — barra fija que sigue al teclado, con
  selector de encabezados y selector de listas (con viñetas/numerada).

## Descarga

El APK más reciente se publica con cada release:

- [**MarkNote-1.0.5.apk**](releases/MarkNote-1.0.5.apk) (también adjunto a la
  [GitHub Release](https://github.com/Ninewansen/MarkNote/releases))

Se instala directamente en Android 8.0+ (API 26+). No requiere servicios de Google Play.

## Compilar desde el código fuente

Requisitos:

- Android Studio (o Android SDK + JDK 17)
- Android SDK Platform 36

```bash
git clone git@github.com:Ninewansen/MarkNote.git
cd MarkNote
./gradlew :app:assembleDebug
```

La firma de la versión release se lee de `keystore.properties` en la raíz del proyecto
(**no se sube al repositorio**). Para un APK release firmado, crea ese archivo localmente:

```properties
storeFile=keystore/marknote.keystore
storePassword=tu-contraseña-del-store
keyAlias=tu-alias
keyPassword=tu-contraseña-de-la-clave
```

Sin él, `assembleRelease` genera un APK sin firmar. Nunca subas tu keystore al repositorio.

## Sincronización WebDAV

1. Abre el menú de la app → **Sincronización WebDAV**.
2. Completa la **URL del servidor** (una dirección `https://…` completa), el **nombre de
   usuario** y la **contraseña**.
3. Toca **Sincronizar ahora** (o activa **Sincronizar al iniciar**).

La sincronización es segura por diseño: los archivos que faltan se copian, los archivos
cambiados se suben/descargan y nada se elimina automáticamente.

## Localización

| Idioma | Código | README | Estado |
| --- | --- | --- | --- |
| 简体中文 | `zh` | [README_zh.md](README_zh.md) | ✅ |
| English | `en` | [README.md](README.md) | ✅ |
| Français | `fr` | [README_fr.md](README_fr.md) | ✅ |
| Deutsch | `de` | [README_de.md](README_de.md) | ✅ |
| 日本語 | `ja` | [README_ja.md](README_ja.md) | ✅ |
| Español | `es` | [README_es.md](README_es.md) | ✅ |

## Tecnologías

- **Kotlin + Jetpack Compose (Material 3)** — interfaz
- [**Vditor**](https://github.com/Vanessa219/vditor) — motor de edición WYSIWYG / renderizado en vivo
- [**Markwon**](https://github.com/noties/Markwon) — renderizado nativo Spannable para la vista previa
- [**Sora Editor**](https://github.com/Rosemoe/sora-editor) — editor de código fuente con
  resaltado de sintaxis
- **OkHttp** — cliente WebDAV

## Privacidad

- Todas las notas e imágenes se guardan **localmente en tu dispositivo**.
- Las credenciales WebDAV se almacenan en preferencias privadas de la app y solo se envían
  al servidor que configures. Usa HTTPS.
- Sin analíticas, sin seguimiento y sin llamadas de red salvo cuando sincronizas.

## Licencia

Publicado bajo la [licencia MIT](LICENSE).

## Agradecimientos

Gracias a los proyectos de código abierto que hacen posible MarkNote: Vditor, Markwon,
Sora Editor, Prism4j y OkHttp. El diseño de interacción está inspirado en Notion y Typora.
