package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Pendiente en iOS (UIActivityViewController): no se puede compilar desde Windows. */
@Composable
actual fun rememberFileSharer(): FileSharer = remember { FileSharer { _, _, _ -> false } }
