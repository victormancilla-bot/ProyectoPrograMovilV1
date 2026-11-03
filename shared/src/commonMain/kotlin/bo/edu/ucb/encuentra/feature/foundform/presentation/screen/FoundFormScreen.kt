package bo.edu.ucb.encuentra.feature.foundform.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.domain.model.CampusZone
import bo.edu.ucb.encuentra.core.presentation.RegisterStepper
import bo.edu.ucb.encuentra.core.domain.model.CustodyPlace
import bo.edu.ucb.encuentra.feature.foundform.domain.usecase.FoundFieldError
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormEffect
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormEvent
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormState
import bo.edu.ucb.encuentra.feature.foundform.presentation.viewmodel.FoundFormViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.components.textfields.AppTextField
import edu.ucb.ds.components.textfields.SelectField
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** "Registrar - Formulario de Objeto" (H03): el staff registra un objeto encontrado, queda DISPONIBLE. */
@Composable
fun FoundFormScreen(
    viewModel: FoundFormViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FoundFormEffect.Registered -> onRegistered()
            }
        }
    }

    FoundFormContent(state = state, onEvent = viewModel::handleEvent, onNavigateBack = onNavigateBack)
}

@Composable
private fun FoundFormContent(
    state: FoundFormState,
    onEvent: (FoundFormEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val spacing = UcbEncuentraTheme.spacing
    val draft = state.draft
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().imePadding()) {
            CurvedHeader(
                title = "Registrar objeto",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Volver",
                onNavigationClick = onNavigateBack,
                centerTitle = true
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenPadding, vertical = spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                RegisterStepper(activeSteps = setOf(2, 3))
                AppBody("Ingresa los detalles del hallazgo.")
                CapturedPhotosRow(state.draft.photos, state.errorFor(FoundFieldError.PHOTO_REQUIRED))

                DetectedDataCard(state = state, onEvent = onEvent)

                AppTextField(
                    value = draft.title,
                    onValueChange = { onEvent(FoundFormEvent.OnTitleChanged(it)) },
                    label = "¿Qué es?",
                    placeholder = "Ej. Smartphone negro",
                    errorMessage = state.errorFor(FoundFieldError.TITLE_TOO_SHORT, FoundFieldError.TITLE_TOO_LONG)
                )
                AppTextField(
                    value = draft.brand,
                    onValueChange = { onEvent(FoundFormEvent.OnBrandChanged(it)) },
                    label = "Marca (opcional)",
                    placeholder = "Ej. Apple, Samsung, Lenovo...",
                    errorMessage = state.errorFor(FoundFieldError.BRAND_TOO_LONG)
                )
                AppTextField(
                    value = draft.features,
                    onValueChange = { onEvent(FoundFormEvent.OnFeaturesChanged(it)) },
                    label = "Características distintivas",
                    placeholder = "Funda de silicona, pantalla rota...",
                    errorMessage = state.errorFor(FoundFieldError.FEATURES_TOO_LONG)
                )

                SelectField(
                    options = CampusZone.entries.map { it.label },
                    selected = draft.foundZone?.label,
                    onSelect = { label ->
                        CampusZone.entries.firstOrNull { it.label == label }?.let { onEvent(FoundFormEvent.OnFoundZoneSelected(it)) }
                    },
                    label = "Ubicación del hallazgo",
                    placeholder = "Selecciona un área del campus",
                    errorMessage = state.errorFor(FoundFieldError.FOUND_ZONE_REQUIRED)
                )
                SelectField(
                    options = CustodyPlace.entries.map { it.label },
                    selected = draft.custodyPlace?.label,
                    onSelect = { label ->
                        CustodyPlace.entries.firstOrNull { it.label == label }?.let { onEvent(FoundFormEvent.OnCustodySelected(it)) }
                    },
                    label = "Ubicación de custodia actual",
                    placeholder = "¿Dónde se encuentra el objeto ahora?",
                    errorMessage = state.errorFor(FoundFieldError.CUSTODY_REQUIRED)
                )
                if (draft.custodyPlace != null) {
                    AppTextField(
                        value = draft.custodyDetail,
                        onValueChange = { onEvent(FoundFormEvent.OnCustodyDetailChanged(it)) },
                        label = "Detalle de custodia (opcional)",
                        placeholder = "Ej. Locker 12, estante B"
                    )
                }

                FoundDateTimeFields(state = state, onEvent = onEvent)

                ControlQuestions(state = state, onEvent = onEvent)
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
                    .background(UcbEncuentraTheme.colors.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                FieldError(
                    state.error ?: if (state.showErrors && state.errors.isNotEmpty()) "Revisa los campos marcados" else null
                )
                PrimaryButton(
                    text = if (state.saving) "Registrando..." else "Registrar en base de datos",
                    onClick = { onEvent(FoundFormEvent.OnSubmit) },
                    enabled = !state.saving,
                    size = AppButtonSize.Large,
                    leadingIcon = Icons.Filled.AddCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Fotos del paso 1 (H01). Para cambiarlas se vuelve atrás: la captura conserva las fotos. */
@Composable
private fun CapturedPhotosRow(photos: List<String>, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
        AppOverline("Fotos (${photos.size})", color = UcbEncuentraTheme.colors.onSurface)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.medium)) {
            photos.forEach { photo ->
                AsyncImage(
                    model = photo,
                    contentDescription = "Foto del objeto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(UcbEncuentraTheme.shapes.small)
                )
            }
        }
        if (photos.isEmpty()) AppBody("Vuelve al paso 1 para tomar al menos una foto.")
        FieldError(error)
    }
}
