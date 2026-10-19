package com.polotno.android

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Base64

/** Writes an exported PNG or PDF to the public Downloads folder. Returns the file name. */
fun saveExportToDownloads(context: Context, export: EditorMessage.Export): String {
    val isPdf = export.format == "pdf"
    val fileName = "polotno-design-${System.currentTimeMillis()}.${if (isPdf) "pdf" else "png"}"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, if (isPdf) "application/pdf" else "image/png")
    }
    // MediaStore.Downloads needs no storage permission (Android 10+).
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create $fileName in Downloads")
    resolver.openOutputStream(uri)!!.use { it.write(Base64.decode(export.base64, Base64.DEFAULT)) }
    return fileName
}
