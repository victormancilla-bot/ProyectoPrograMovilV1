package bo.edu.ucb.encuentra.core.presentation

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android: escribe el archivo en `cache/exports` (compartido por el FileProvider de la app, ver
 * `res/xml/file_paths.xml`) y abre el menú "Compartir" del sistema con permiso de lectura temporal.
 */
@Composable
actual fun rememberFileSharer(): FileSharer {
    val context = LocalContext.current
    return remember(context) {
        FileSharer { fileName, mimeType, content ->
            runCatching {
                val folder = File(context.cacheDir, EXPORTS).apply { mkdirs() }
                // Se borran exportaciones anteriores: el caché no acumula datos personales.
                folder.listFiles()?.forEach { it.delete() }
                val file = File(folder, fileName).apply { writeText(content, Charsets.UTF_8) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND)
                    .setType(mimeType)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, fileName)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(
                    Intent.createChooser(send, "Compartir $fileName").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { Log.w(EXPORT_TAG, "No se pudo compartir $fileName", it) }.isSuccess
        }
    }
}

private const val EXPORTS = "exports"
private const val EXPORT_TAG = "UcbExport"
