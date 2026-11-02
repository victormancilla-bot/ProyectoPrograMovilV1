package bo.edu.ucb.encuentra.feature.reportwizard.domain.usecase

import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import bo.edu.ucb.encuentra.core.domain.repository.MatchingTrigger
import bo.edu.ucb.encuentra.core.domain.usecase.GetCurrentUserUseCase
import bo.edu.ucb.encuentra.core.domain.vo.ItemId
import bo.edu.ucb.encuentra.feature.reportwizard.domain.model.ReportDraft
import bo.edu.ucb.encuentra.feature.reportwizard.domain.repository.LostReportRepository
import kotlin.time.Clock

/**
 * Envío del wizard (L04): revisa los 3 pasos, guarda el reporte como BUSCANDO y dispara el matching (M01).
 * Si el matching falla, el reporte igual queda guardado (se puede volver a comparar después).
 */
class SubmitLostReportUseCase(
    private val validate: ValidateReportStepUseCase,
    private val repository: LostReportRepository,
    private val matching: MatchingTrigger,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val clock: Clock
) {
    suspend operator fun invoke(draft: ReportDraft): Result<LostReport> = runCatching {
        val user = getCurrentUser() ?: throw IllegalStateException(NO_SESSION)
        val errors = validate.upTo(ValidateReportStepUseCase.STEP_WHERE_WHEN, draft)
        if (errors.isNotEmpty()) throw IllegalArgumentException(errors.first().message)
        val (lostFrom, lostTo) = requireNotNull(validate.lostRange(draft))

        val report = LostReport(
            id = ItemId.generate(),
            ownerId = user.id,
            title = draft.title.trim(),
            category = requireNotNull(draft.category),
            subcategory = requireNotNull(draft.subcategory),
            color = draft.color,
            brand = draft.brand.trim().ifEmpty { null },
            description = draft.features.trim(),
            campus = user.campus,
            zone = requireNotNull(draft.zone),
            locationDetail = draft.locationDetail.trim().ifEmpty { null },
            lostFrom = lostFrom,
            lostTo = lostTo,
            photoUrl = draft.photoUri,
            contactPhone = draft.contactPhone.filterNot { it == ' ' || it == '-' }.ifEmpty { null },
            status = ReportStatus.BUSCANDO,
            createdAt = clock.now()
        )
        repository.save(report)
        runCatching { matching.onReportCreated(report) }
        report
    }

    companion object {
        const val NO_SESSION = "Tu sesión expiró, vuelve a ingresar"
    }
}
