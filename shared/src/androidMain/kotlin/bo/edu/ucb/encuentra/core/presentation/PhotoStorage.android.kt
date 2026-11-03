package bo.edu.ucb.encuentra.core.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import bo.edu.ucb.encuentra.core.util.PhotoCompression
import bo.edu.ucb.encuentra.core.util.PhotoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Lee la foto de [source] (cámara o galería), la gira según su EXIF, la reduce con [PhotoCompression] y la guarda
 * como JPEG en `filesDir/<folder>`. Devuelve `file://...` o null si no se pudo leer.
 * Se guarda una copia propia porque el permiso de lectura del URI de la galería se pierde al cerrar la app.
 */
@OptIn(ExperimentalUuidApi::class)
internal suspend fun compressToAppStorage(context: Context, source: Uri, folder: PhotoFolder): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            // Con inJustDecodeBounds, decodeStream devuelve null a propósito: solo llena outWidth/outHeight.
            val stream = resolver.openInputStream(source) ?: error("No se pudo abrir $source")
            stream.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("No es una imagen válida: $source")

            val options = BitmapFactory.Options().apply {
                inSampleSize = PhotoCompression.sampleSize(bounds.outWidth, bounds.outHeight)
            }
            val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("No se pudo decodificar $source")
            val rotation = resolver.openInputStream(source)?.use { exifRotation(ExifInterface(it)) } ?: 0f
            val (width, height) = PhotoCompression.targetSize(decoded.width, decoded.height)
            val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)
            val upright = if (rotation == 0f) {
                scaled
            } else {
                Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, Matrix().apply { postRotate(rotation) }, true)
            }

            val dir = File(context.filesDir, folder.path).apply { mkdirs() }
            val file = File(dir, "${Uuid.random()}.jpg")
            file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, PhotoCompression.JPEG_QUALITY, it) }
            listOf(decoded, scaled, upright).distinct().forEach { it.recycle() }
            Uri.fromFile(file).toString()
        }.onFailure { Log.w(TAG, "No se pudo comprimir la foto $source", it) }.getOrNull()
    }

internal const val TAG = "UcbPhotos"

/** Las cámaras guardan la foto "acostada" y anotan la rotación en el EXIF. */
private fun exifRotation(exif: ExifInterface): Float = when (
    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
    else -> 0f
}
