package bo.edu.ucb.encuentra.feature.myreports.presentation.screen

import androidx.compose.runtime.Composable
import bo.edu.ucb.encuentra.core.domain.model.LostReport
import bo.edu.ucb.encuentra.core.domain.model.ReportStatus
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone

/** Chip de estado del mockup ("Match confirmado"). "Buscando" no lleva chip: es el estado normal. */
internal fun ReportStatus.statusChip(): (@Composable () -> Unit)? = when (this) {
    ReportStatus.BUSCANDO -> null
    ReportStatus.EN_VERIFICACION -> { { StatusChip(label, StatusTone.Warning) } }
    ReportStatus.MATCH_CONFIRMADO, ReportStatus.RECUPERADO -> { { StatusChip(label, StatusTone.Success) } }
    ReportStatus.CERRADO, ReportStatus.VENCIDO -> { { StatusChip(label, StatusTone.Neutral) } }
}

/** "Biblioteca Central, Piso 2" (zona + detalle si lo hay). */
internal val LostReport.locationText: String
    get() = listOfNotNull(zone.label, locationDetail?.takeIf { it.isNotBlank() }).joinToString(", ")
