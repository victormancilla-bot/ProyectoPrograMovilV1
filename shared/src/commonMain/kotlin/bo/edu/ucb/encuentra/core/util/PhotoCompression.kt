package bo.edu.ucb.encuentra.core.util

/**
 * Compresión de fotos antes de guardarlas (H01): una foto de cámara pesa 3-8 MB; reducida al lado mayor de
 * [MAX_SIDE] px y en JPEG [JPEG_QUALITY] queda en ~200-400 KB, suficiente para reconocer el objeto y para subirla
 * después a Storage (H06). La parte de cálculo es común y se prueba aquí; decodificar y escribir es de cada plataforma.
 */
object PhotoCompression {
    const val MAX_SIDE = 1600
    const val JPEG_QUALITY = 80

    /** Tamaño final manteniendo la proporción; una foto más chica que [maxSide] no se agranda. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        require(width > 0 && height > 0) { "Tamaño inválido: ${width}x$height" }
        val longest = maxOf(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toDouble() / longest
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }

    /**
     * Factor de submuestreo (potencia de 2) para decodificar sin cargar la foto completa en memoria: la mayor
     * potencia de 2 que todavía deja la imagen igual o más grande que [maxSide]. Luego se escala al tamaño exacto.
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }
}

/** Carpetas privadas de la app donde se guardan las fotos ya comprimidas. */
enum class PhotoFolder(val path: String) {
    /** Fotos de referencia de los reportes de perdidos (L03). */
    REPORTS("report_photos"),

    /** Fotos de los objetos encontrados (H01). */
    FOUND_ITEMS("found_photos")
}
