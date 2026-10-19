# Polotno Android Demo

Native Kotlin + Jetpack Compose sample app that embeds the Polotno editor (React + Vite) in an Android `WebView`. The app passes design JSON to the editor, receives the saved design and a PNG preview back, and saves PNG/PDF exports to the device.

## Prerequisites

- Android Studio Ladybug or newer (JDK 17, Android SDK 35)
- Android 10+ device or emulator (minSdk 29)
- Node.js 18+ and npm

## Project layout

| Path            | Description                                                 |
| --------------- | ----------------------------------------------------------- |
| `app/`          | Kotlin sources, resources, sample design, bundled editor    |
| `web-editor/`   | Vite React app that hosts the Polotno editor                |
| `gradle/`       | Version catalog (`libs.versions.toml`) and Gradle wrapper   |

Kotlin sources in `app/src/main/java/com/polotno/android/`:

| File               | Role                                                                  |
| ------------------ | --------------------------------------------------------------------- |
| `MainActivity.kt`  | Home screen with the last saved preview and **Open Editor**           |
| `EditorScreen.kt`  | Full-screen editor: app bar, file picker, back-button dialog, messages |
| `EditorWebView.kt` | WebView setup, `WebViewAssetLoader`, navigation lock                  |
| `EditorBridge.kt`  | `window.PolotnoAndroid` – messages from the editor                    |
| `ExportSaver.kt`   | Writes PNG/PDF exports to the Downloads folder                        |
| `DesignStorage.kt` | Keeps the latest design JSON and preview in app storage               |

## Building the embedded editor

```bash
cd ./web-editor
npm install
npm run build:android
```

`npm run build:android` runs `vite build` and copies the single-file bundle into `app/src/main/assets/editor/`. The Android build fails with a clear message if that step was skipped.

> **Note:** Set your Polotno API key before building. Create one at [polotno.com/cabinet](https://polotno.com/cabinet), then add `VITE_POLOTNO_KEY=your-key` to `web-editor/.env.local` (or replace the fallback in `App.jsx`). In the cabinet, add `https://appassets.androidplatform.net` to the key's allowed domains – that is the origin the editor runs on inside the app.

## Running the Android app

1. Open the project root in Android Studio and let Gradle sync.
2. Select an emulator or device.
3. Run the `app` configuration.
4. Tap **Open Editor**. **Save** posts `{ docJson, previewBase64 }` to Kotlin through `window.PolotnoAndroid`; the home screen shows the returned PNG and the next session starts from the saved design. **PNG** and **PDF** export the design to the Downloads folder.

From the command line:

```bash
./gradlew installDebug
```

## How it works

### WebView configuration

- **WebViewAssetLoader**: the bundle is served from `https://appassets.androidplatform.net/assets/editor/index.html`, a regular https origin. File access stays disabled (`allowFileAccess = false`).
- **Single-file bundle**: `vite-plugin-singlefile` inlines all JS and CSS into one `index.html`, the same bundle shape as the [iOS demo](https://github.com/polotno-project/polotno-swift).
- **Navigation lock**: the WebView only loads the editor page. Other links open in the browser, so the JavaScript interface is never exposed to another site.
- **Rotation**: the activity handles configuration changes itself, so rotating the device keeps the editor and unsaved edits.
- **Release builds**: `proguard-rules.pro` keeps `EditorBridge` with `-keep`. With only `-keepclassmembers`, R8 drops `postMessage` and the bridge stops working in release builds only.
- **Debugging**: debug builds enable `chrome://inspect` for the WebView. WebView also logs page console messages to Logcat under the `chromium` tag.

### Image uploads

Polotno's Upload panel uses `<input type="file">`. Android WebView ignores it unless the app implements `WebChromeClient.onShowFileChooser`. `EditorScreen` opens the system picker with `FileChooserParams.createIntent()` and returns the result with `FileChooserParams.parseResult()`. The callback is always answered, with `null` on cancel, or the input stops responding.

Uploaded images are stored in the design JSON as data URLs. For production, upload the file to your own storage and put its URL in the design instead.

### Exports

Polotno's toolbar download button relies on a browser download, which does nothing in a WebView, so `App.jsx` leaves it off inside the app. Instead, a native app bar above the WebView (`EditorScreen.kt`) has **Close**, **PNG**, **PDF** and **Save**. It calls `window.polotnoNative.export('png' | 'pdf')` with `evaluateJavascript`; the editor runs `store.toDataURL()` / `store.toPDFDataURL()` and posts `{ type: "export", format, base64 }` back. `ExportSaver.kt` writes the file to Downloads through `MediaStore`, which needs no storage permission.

Keeping these actions native also leaves the web toolbar its full width on a phone screen.

### Online requirement

The editor code is bundled in the app, but Polotno loads stock photos, templates, fonts and the PDF library from the network. The device must be online for these features.

> **Note for Enterprise Clients**: Full offline support (bundling all assets and fonts) is available for enterprise licenses. Contact support for details.

### Limitations

- **Memory**: exports travel through the bridge as base64 strings. Large multi-page PDFs or high pixel ratios can use a lot of memory on low-end devices. If the WebView renderer is killed, the app returns to the home screen and keeps the last save.

## Data flow

1. Kotlin keeps the latest design JSON in app storage (`DesignStorage`), falling back to `res/raw/sample_document.json`.
2. When the editor script loads, it posts `{ type: "ready" }` through `window.PolotnoAndroid.postMessage`.
3. Kotlin answers with `evaluateJavascript("window.polotnoNative.load(<json>)")`. A JSON document is a valid JavaScript object literal, so it goes in as is, with no escaping or encoding.
4. **Save** (in the app bar or the back-button dialog) calls `window.polotnoNative.save()`. The editor serializes the store, renders a PNG preview and posts `{ type: "save", docJson, previewBase64 }`. If saving or exporting throws, it posts `{ type: "error" }` instead.
5. Kotlin writes both to app storage and shows the preview on the home screen.

## Customization ideas

- Replace `res/raw/sample_document.json` with your own design or load it from your backend.
- Change the editor UI in `web-editor/src/App.jsx`: side panels, toolbar actions, save flows.
- Add message types to `EditorBridge` (analytics, autosave, uploading to your server).
