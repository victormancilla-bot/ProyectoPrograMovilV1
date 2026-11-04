package bo.edu.ucb.encuentra.core.util

/**
 * D03 · CSV que abre bien en Excel en español y en Google Sheets:
 * - separador `;` (el Excel con configuración regional de Bolivia usa la coma como decimal y no separa por comas);
 * - BOM UTF-8 al inicio, para que los acentos y la ñ se vean bien;
 * - comillas dobles cuando el texto trae separador, comillas o saltos de línea (RFC 4180);
 * - un apóstrofo delante de lo que empieza con = + - @, para que una descripción no se ejecute como fórmula
 *   (inyección de fórmulas en hojas de cálculo).
 */
object Csv {
    const val BOM = "﻿"
    const val DELIMITER = ';'
    private const val NEWLINE = "\r\n"
    private val FORMULA_START = setOf('=', '+', '-', '@', '\t', '\r')

    fun build(header: List<String>, rows: List<List<String>>): String = buildString {
        append(BOM)
        append(line(header))
        rows.forEach { append(NEWLINE).append(line(it)) }
        append(NEWLINE)
    }

    private fun line(cells: List<String>): String = cells.joinToString(DELIMITER.toString()) { cell(it) }

    internal fun cell(raw: String): String {
        val safe = if (raw.isNotEmpty() && raw.first() in FORMULA_START) "'$raw" else raw
        val needsQuotes = safe.any { it == DELIMITER || it == '"' || it == '\n' || it == '\r' || it == ',' }
        return if (needsQuotes) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
