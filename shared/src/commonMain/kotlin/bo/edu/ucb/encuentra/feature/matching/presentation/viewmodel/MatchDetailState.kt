package bo.edu.ucb.encuentra.feature.matching.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.matching.domain.model.MatchDetail

/** Detalle de match (M05): cargando → contenido / no disponible / error. */
data class MatchDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: MatchDetail? = null,
    /** "No es mío" tocado: se pide confirmación porque no se puede deshacer. */
    val confirmingReject: Boolean = false,
    /** Reclamando o descartando: los botones se deshabilitan para no enviar dos veces. */
    val working: Boolean = false,
    /** Número del reclamo creado ("Reclamo #1026"): reemplaza los botones por la confirmación. */
    val claimNumber: Int? = null,
    /** Error de "Esto es mío" o "No es mío" (el detalle sigue visible). */
    val actionError: String? = null,
    /** Borrador de respuestas a las preguntas de control del reclamo (V01), una por pregunta. */
    val answers: List<String> = emptyList(),
    /** Resultado del último envío de respuestas: "no coinciden, te quedan N intentos" o un error. */
    val answersFeedback: String? = null
) {
    val canSubmitAnswers: Boolean get() = !working && answers.isNotEmpty() && answers.all { it.isNotBlank() }

    /** No existe, fue descartada o no es del usuario: se muestra "no disponible", nunca datos ajenos. */
    val notFound: Boolean get() = !loading && error == null && detail == null

    val canAct: Boolean get() = detail?.canAct == true && claimNumber == null
}

sealed class MatchDetailEvent {
    data object OnBack : MatchDetailEvent()
    data object OnRetry : MatchDetailEvent()

    /** "Esto es mío — verificar": crea el reclamo (V02). */
    data object OnClaim : MatchDetailEvent()

    /** Preguntas de control (V01). */
    data class OnAnswerChanged(val index: Int, val text: String) : MatchDetailEvent()
    data object OnSubmitAnswers : MatchDetailEvent()

    /** "No es lo mío, seguir buscando": pide confirmación y la coincidencia se descarta. */
    data object OnReject : MatchDetailEvent()
    data object OnConfirmReject : MatchDetailEvent()
    data object OnDismissReject : MatchDetailEvent()
}

sealed class MatchDetailEffect {
    /** Atrás, o después de "No es mío" (vuelve a Posibles coincidencias, que ya no la muestra). */
    data object NavigateBack : MatchDetailEffect()
}
