# Keep methods the editor calls through addJavascriptInterface.
# -keepclassmembers isn't enough: R8 full mode still merged the class
# and dropped postMessage, which breaks the bridge only in release builds.
-keep class com.polotno.android.EditorBridge {
    @android.webkit.JavascriptInterface <methods>;
}
