package bo.edu.ucb.encuentra.feature.foundform.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.FoundItem
import bo.edu.ucb.encuentra.core.domain.model.ItemStatus
import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.foundform.domain.model.FoundItemDraft
import bo.edu.ucb.encuentra.feature.foundform.domain.repository.FoundItemRepository
import bo.edu.ucb.encuentra.core.domain.usecase.GetAppConfigUseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Registra un objeto encontrado (H03): valida el borrador y lo guarda en Room con estado DISPONIBLE.
 * La sede y quién lo registró salen de la sesión del staff. Después dispara el matching (M01).
 */
class RegisterFoundItemUseCase(
    private val validate: ValidateFoundItemUseCase,
    private val repository: FoundItemRepository,
    private val matching: MatchingTrigger,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock,
    /** F08: los días de retención los define la oficina en Remote Config (`retention_days`). */
    private val getAppConfig: GetAppConfigUseCase
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
            controlQuestions = draft.questionsWithAnswers.map { it.first },
            expectedAnswers = draft.questionsWithAnswers.map { it.second },
            registeredBy = user.id,
            retentionUntil = now + getAppConfig().retentionDays.days,
            createdAt = now
        )
        return runCatching { repository.register(item) }.map {
            // Busca reportes de perdido que coincidan (M01); si falla, el objeto igual queda registrado.
            runCatching { matching.onFoundItemRegistered(item) }
            item
        }
    }

    companion object {
        const val NO_SESSION = "Tu sesión expiró, vuelve a ingresar"
        const val NOT_STAFF = "Solo el staff puede registrar objetos encontrados"
    }
}
