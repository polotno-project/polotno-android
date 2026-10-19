package com.polotno.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File

/**
 * Keeps the latest design JSON and preview PNG in app storage.
 * Files rather than Intent extras: designs easily pass the ~1 MB Binder limit.
 */
class DesignStorage(context: Context) {
    private val appContext = context.applicationContext
    private val docFile = File(appContext.filesDir, "design.json")
    private val previewFile = File(appContext.filesDir, "preview.png")

    fun loadDocJson(): String =
        if (docFile.exists()) docFile.readText()
        else appContext.resources.openRawResource(R.raw.sample_document).bufferedReader().use { it.readText() }

    fun loadPreview(): Bitmap? =
        if (previewFile.exists()) BitmapFactory.decodeFile(previewFile.path) else null

    fun save(payload: EditorMessage.Save) {
        docFile.writeText(payload.docJson)
        if (payload.previewBase64.isNotEmpty()) {
            previewFile.writeBytes(Base64.decode(payload.previewBase64, Base64.DEFAULT))
        }
    }
}
