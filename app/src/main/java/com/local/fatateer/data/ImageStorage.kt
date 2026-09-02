package com.local.fatateer.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * ينسخ صورة المنتج المختارة من معرض الصور إلى تخزين التطبيق الخاص،
 * حتى تبقى الصورة متاحة دائمًا حتى لو انتقلت أو انحذفت من المعرض الأصلي.
 */
object ImageStorage {
    private const val DIR_NAME = "item_images"

    suspend fun copyToAppStorage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }
            val outFile = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            
            // Delete the source file if it's a temporary camera file in cacheDir
            if (uri.path?.contains("cache") == true) {
                try {
                    val tempFile = File(uri.path!!) 
                    // Note: This is a simple check, FileProvider URIs are different.
                    // Better to let the calling code handle the temporary file deletion.
                } catch (_: Exception) {}
            }
            
            outFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
            // ignore cleanup failures
        }
    }
}
