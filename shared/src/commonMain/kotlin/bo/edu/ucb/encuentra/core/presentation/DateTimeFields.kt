package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.theme.UcbEncuentraTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Campo de solo lectura que abre un selector al tocarlo (fecha, hora). Mismo estilo que [AppTextField]. */
@Composable
fun ClickableField(
    value: String,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    errorMessage: String? = null
) {
    Box(modifier) {
        AppTextField(value = value, onValueChange = {}, label = label, leadingIcon = icon, errorMessage = errorMessage)
        Box(Modifier.matchParentSize().clickable(onClick = onClick))
    }
}

/** Calendario de Material 3 que solo deja elegir hasta hoy (hallazgos y pérdidas no pueden ser futuros). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastDatePickerDialog(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // El DatePicker trabaja en milisegundos UTC a medianoche.
    val todayMillis = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toUtcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPick(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                }
                onDismiss()
            }) { DialogAction("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { DialogAction("Cancelar") } }
    ) {
        DatePicker(state = state)
    }
}

/** Reloj de Material 3 en formato 24 h. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime?, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial?.hour ?: 0, initialMinute = initial?.minute ?: 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime(state.hour, state.minute))
                onDismiss()
            }) { DialogAction("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { DialogAction("Cancelar") } },
        text = { TimePicker(state = state) }
    )
}

@Composable
private fun DialogAction(text: String) = AppLabel(text, color = UcbEncuentraTheme.colors.primary)

private fun Int.pad() = toString().padStart(2, '0')

/** dd/mm/aaaa (la tarjeta L03 corrige el mm/dd/yyyy del mockup). */
fun LocalDate.formatDmy() = "${day.pad()}/${month.number.pad()}/$year"

fun LocalTime.formatHm() = "${hour.pad()}:${minute.pad()}"

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)

/** "12 oct 2026" (tarjetas de listas, como "Encontrado: 12 Oct 2023" del mockup). */
fun LocalDate.formatShort() = "$day ${MONTHS[month.number - 1].take(3)} $year"

/** Fecha local de un instante, en la zona del dispositivo. */
fun Instant.toLocalDate(): LocalDate = toLocalDateTime(TimeZone.currentSystemDefault()).date

/** "2 de noviembre de 2026" (resúmenes y detalle). */
fun LocalDate.formatLong() = "$day de ${MONTHS[month.number - 1]} de $year"

/** Rango legible: "2 de noviembre de 2026" si es un día, "30 de octubre al 2 de noviembre de 2026" si son varios. */
fun formatDateRange(from: LocalDate, to: LocalDate): String = when {
    from == to -> from.formatLong()
    from.year == to.year -> "${from.day} de ${MONTHS[from.month.number - 1]} al ${to.formatLong()}"
    else -> "${from.formatLong()} al ${to.formatLong()}"
}

private fun LocalDate.toUtcMillis() = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
