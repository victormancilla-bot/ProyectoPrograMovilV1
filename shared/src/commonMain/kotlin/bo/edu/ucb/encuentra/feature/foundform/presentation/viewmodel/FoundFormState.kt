package bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel

import bo.edu.ucb.encuentra.feature.foundform.domain.model.FoundItemDraft
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.FoundFieldError
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.ValidateFoundItemUseCase

data class FoundFormState(
    val draft: FoundItemDraft = FoundItemDraft(),
    /** Errores actuales del formulario (se recalculan con cada cambio). */
    val errors: Set<FoundFieldError> = emptySet(),
    /** Los errores se muestran recién después del primer intento de registrar. */
    val showErrors: Boolean = false,
    val showAllColors: Boolean = false,
    val saving: Boolean = false,
    /** Error al guardar (sesión, Room); los de campos van debajo de cada campo. */
    val error: String? = null
) {
    val canAddQuestion: Boolean get() = draft.controlQuestions.size < ValidateFoundItemUseCase.MAX_QUESTIONS

    /** Mensaje del primer error de [candidates] que esté visible, o null. */
    fun errorFor(vararg candidates: FoundFieldError): String? =
        if (!showErrors) null else candidates.firstOrNull { it in errors }?.message
}
