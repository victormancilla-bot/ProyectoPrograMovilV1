package bo.edu.ucb.encuentra.core.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Ejecuta [block] y convierte cualquier error en Result.failure (igual que en ucbp1_project_modified).
 * CancellationException se relanza para no romper la cancelación de corrutinas.
 *
 * Se atrapa Exception a propósito: Firebase lanza excepciones de muchos tipos (por ejemplo, si no hay
 * `google-services.json`) y aquí es el límite donde se convierten en Result.
 */
@Suppress("TooGenericExceptionCaught")
inline fun <T> safeCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
