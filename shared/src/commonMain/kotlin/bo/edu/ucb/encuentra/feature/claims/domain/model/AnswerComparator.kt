package bo.edu.ucb.encuentra.feature.claims.domain.model

/** Resultado de comparar una respuesta del estudiante con la esperada (V01). Es una SUGERENCIA: decide el staff. */
enum class AnswerVerdict(val label: String) {
    COINCIDE("Coincide"),
    NO_COINCIDE("No coincide"),
    /** El objeto no tiene respuesta esperada (registrado antes de V01): solo decide el staff. */
    SIN_REFERENCIA("Sin respuesta esperada")
}

/**
 * Comparación asistida (V01): tolera mayúsculas, tildes, signos y palabras de relleno.
 * Coincide si la respuesta contiene la esperada (o al revés) o si comparte al menos la mitad de sus palabras clave:
 * "uno de la UCB" vs "Escudo de la UCB" → coincide; "Rojo" vs "Café" → no coincide.
 */
object AnswerComparator {

    fun compare(answer: String, expected: String?): AnswerVerdict {
        if (expected.isNullOrBlank()) return AnswerVerdict.SIN_REFERENCIA
        val a = normalize(answer)
        val e = normalize(expected)
        if (a.isEmpty()) return AnswerVerdict.NO_COINCIDE
        if (a == e || (e.length >= MIN_CONTAINED && a.contains(e)) || (a.length >= MIN_CONTAINED && e.contains(a))) {
            return AnswerVerdict.COINCIDE
        }
        val expectedWords = keywords(e)
        if (expectedWords.isEmpty()) return AnswerVerdict.NO_COINCIDE
        val shared = expectedWords.count { it in keywords(a) }
        return if (shared * 2 >= expectedWords.size) AnswerVerdict.COINCIDE else AnswerVerdict.NO_COINCIDE
    }

    /** Minúsculas, sin tildes y solo letras/números separados por un espacio. */
    internal fun normalize(text: String): String =
        text.lowercase()
            .map { ACCENTS[it] ?: it }
            .joinToString("")
            .replace(Regex("[^a-z0-9ñ]+"), " ")
            .trim()

    internal fun keywords(normalized: String): Set<String> =
        normalized.split(' ').filter { it.isNotEmpty() && it !in STOPWORDS && (it.length >= 2 || it.all(Char::isDigit)) }.toSet()

    private const val MIN_CONTAINED = 3
    private val ACCENTS = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u')
    private val STOPWORDS = setOf(
        "de", "la", "el", "los", "las", "un", "una", "uno", "unos", "unas", "y", "o", "con", "del", "al", "en", "mi",
        "es", "era", "que", "por", "para", "tiene", "tenia", "habia", "hay", "su", "sus", "lo", "le", "se", "muy"
    )
}
