package com.polotno.android

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONException
import org.json.JSONObject

/** Message the editor posts with `window.PolotnoAndroid.postMessage(json)`. */
sealed interface EditorMessage {
    data object Ready : EditorMessage
    data class Save(val docJson: String, val previewBase64: String) : EditorMessage
    data class Export(val format: String, val base64: String) : EditorMessage
    data class Error(val message: String) : EditorMessage
}

/**
 * Exposed to JavaScript as `window.PolotnoAndroid`.
 *
 * WebView calls @JavascriptInterface methods on a background thread, so each
 * message is parsed there and handed to [onMessage] on the main thread.
 */
class EditorBridge(private val onMessage: (EditorMessage) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(json: String) {
        val message = try {
            val obj = JSONObject(json)
            when (obj.getString("type")) {
                "ready" -> EditorMessage.Ready
                "save" -> EditorMessage.Save(obj.getString("docJson"), obj.getString("previewBase64"))
                "export" -> EditorMessage.Export(obj.getString("format"), obj.getString("base64"))
                else -> EditorMessage.Error(obj.optString("message"))
            }
        } catch (error: JSONException) {
            Log.e("EditorBridge", "Invalid editor message", error)
            return
        }
        mainHandler.post { onMessage(message) }
    }

    companion object {
        const val JS_NAME = "PolotnoAndroid"
    }
}
