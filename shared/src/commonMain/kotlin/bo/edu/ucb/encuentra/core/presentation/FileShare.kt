package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable

/**
 * D03 · Compartir un archivo de texto (CSV) con otra app: correo, WhatsApp, Drive... (expect/actual).
 * Devuelve false si no se pudo (p. ej. iOS, todavía pendiente).
 */
fun interface FileSharer {
    fun share(fileName: String, mimeType: String, content: String): Boolean
}

@Composable
expect fun rememberFileSharer(): FileSharer
