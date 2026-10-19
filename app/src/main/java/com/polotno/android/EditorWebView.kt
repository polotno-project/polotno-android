package com.polotno.android

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/** WebViewAssetLoader serves `app/src/main/assets/editor/index.html` from this https origin. */
private const val EDITOR_HOST = WebViewAssetLoader.DEFAULT_DOMAIN
private const val EDITOR_URL = "https://$EDITOR_HOST/assets/editor/index.html"

/**
 * Builds the WebView that hosts the Polotno editor.
 *
 * @param onShowFileChooser called for `<input type="file">` (the Upload panel).
 *   Return true and answer the callback exactly once, with null on cancel,
 *   or the input stops responding.
 * @param onRendererGone the renderer crashed or was killed to free memory.
 */
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
fun createEditorWebView(
    context: Context,
    bridge: EditorBridge,
    onShowFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Boolean,
    onRendererGone: () -> Unit,
): WebView {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG) // chrome://inspect

    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    return WebView(context).apply {
        // AndroidView defaults to WRAP_CONTENT, which collapses the page's
        // viewport to its content height: a blank, 0px-tall editor.
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false // the bundle comes over https, not file://
        settings.allowContentAccess = false

        // addJavascriptInterface can't be scoped to an origin, so the WebView
        // never leaves the editor page (see shouldOverrideUrlLoading).
        addJavascriptInterface(bridge, EditorBridge.JS_NAME)

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == EDITOR_HOST) return false
                // Other links (e.g. the Polotno credit) open in the browser.
                try {
                    view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (_: ActivityNotFoundException) {
                }
                return true
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                onRendererGone()
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams,
            ) = onShowFileChooser(filePathCallback, fileChooserParams)
        }

        loadUrl(EDITOR_URL)
    }
}

/** Calls `window.polotnoNative[method](arg)` in the editor; [arg] is a JS expression. */
fun WebView.callEditor(method: String, arg: String = "") {
    evaluateJavascript("window.polotnoNative.$method($arg)", null)
}
