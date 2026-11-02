package bo.edu.ucb.encuentra.feature.foundform.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.domain.model.FoundItemDraft
import bo.edu.ucb.encuentra.feature.foundform.domain.repository.FoundItemRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Registra un objeto encontrado (H03): valida el borrador y lo guarda en Room con estado DISPONIBLE.
 * La sede y quién lo registró salen de la sesión del staff.
 */
class RegisterFoundItemUseCase(
    private val validate: ValidateFoundItemUseCase,
    private val repository: FoundItemRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(draft: FoundItemDraft): Result<FoundItem> {
        val user = getCurrentUser() ?: return Result.failure(IllegalStateException(NO_SESSION))
        if (!user.role.isStaff) return Result.failure(IllegalStateException(NOT_STAFF))
        val errors = validate(draft)
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.first().message))

        val now = clock.now()
        val item = FoundItem(
            id = ItemId.generate(),
            title = draft.title.trim(),
            category = requireNotNull(draft.category),
            subcategory = requireNotNull(draft.subcategory),
            color = draft.color,
            brand = draft.brand.trim().ifEmpty { null },
            description = draft.features.trim(),
            campus = user.campus,
            foundZone = requireNotNull(draft.foundZone),
            foundDetail = null,
            foundAt = requireNotNull(validate.foundAt(draft)),
            custodyPlace = requireNotNull(draft.custodyPlace),
            custodyDetail = draft.custodyDetail.trim(),
            status = ItemStatus.DISPONIBLE,
            photoUrls = emptyList(),
            controlQuestions = draft.controlQuestions.map { it.trim() }.filter { it.isNotEmpty() },
            registeredBy = user.id,
            retentionUntil = now + RETENTION,
            createdAt = now
        )
        return runCatching { repository.register(item) }.map { item }
    }

    companion object {
        /** Días que el objeto queda en custodia antes de pasar a donación/descarte (D04). */
        val RETENTION = 90.days
        const val NO_SESSION = "Tu sesión expiró, vuelve a ingresar"
        const val NOT_STAFF = "Solo el staff puede registrar objetos encontrados"
    }
}
