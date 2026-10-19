package com.polotno.android

import android.net.Uri
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Full-screen Polotno editor with a native app bar (Close, PNG, PDF, Save).
 * Loads [initialDocJson] once the page is ready; Back asks before discarding edits.
 */
@Composable
fun EditorScreen(
    initialDocJson: String,
    onSave: (EditorMessage.Save) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnSave by rememberUpdatedState(onSave)
    val currentOnClose by rememberUpdatedState(onClose)
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isReady by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) } // waiting for a save/export reply
    var showExitDialog by remember { mutableStateOf(false) }

    // The Upload panel's pending <input type="file">, answered when the picker returns.
    var pendingUpload by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        pendingUpload?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(it.resultCode, it.data))
        pendingUpload = null
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val bridge = remember {
        EditorBridge { message ->
            isBusy = false
            when (message) {
                EditorMessage.Ready -> {
                    // A JSON document is a valid JS object literal.
                    webView?.callEditor("load", initialDocJson)
                    isReady = true
                }
                is EditorMessage.Save -> currentOnSave(message)
                is EditorMessage.Export -> try {
                    toast("Saved ${saveExportToDownloads(context, message)} to Downloads")
                } catch (error: Exception) {
                    Log.e("EditorScreen", "Export failed", error)
                    toast("Export failed")
                }
                is EditorMessage.Error -> toast("Something went wrong. Please try again.")
            }
        }
    }

    fun request(method: String, arg: String = "") {
        isBusy = true
        webView?.callEditor(method, arg)
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val enabled = isReady && !isBusy
            TextButton(onClick = { showExitDialog = true }) { Text("Close") }
            Spacer(Modifier.weight(1f))
            TextButton(enabled = enabled, onClick = { request("export", "'png'") }) { Text("PNG") }
            TextButton(enabled = enabled, onClick = { request("export", "'pdf'") }) { Text("PDF") }
            Button(enabled = enabled, onClick = { request("save") }) { Text(if (isBusy) "Working…" else "Save") }
        }
        HorizontalDivider()
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { ctx ->
                createEditorWebView(
                    context = ctx,
                    bridge = bridge,
                    onShowFileChooser = { callback, params ->
                        pendingUpload?.onReceiveValue(null)
                        pendingUpload = callback
                        filePicker.launch(params.createIntent())
                        true
                    },
                    onRendererGone = {
                        toast("The editor stopped. Your last save is kept.")
                        currentOnClose()
                    },
                ).also { webView = it }
            },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingUpload?.onReceiveValue(null)
            webView?.destroy()
        }
    }

    BackHandler { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave the editor?") },
            text = { Text("Save your changes before closing?") },
            confirmButton = {
                TextButton(enabled = isReady, onClick = {
                    showExitDialog = false
                    request("save")
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onClose()
                }) { Text("Discard") }
            },
        )
    }
}
