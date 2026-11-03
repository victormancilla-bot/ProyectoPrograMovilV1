package bo.edu.ucb.encuentra.feature.foundcapture.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bo.edu.ucb.encuentra.core.presentation.AppPermission
import bo.edu.ucb.encuentra.core.presentation.CameraLauncher
import bo.edu.ucb.encuentra.core.presentation.PermissionRationaleDialog
import bo.edu.ucb.encuentra.core.presentation.CameraPermissionResult
import bo.edu.ucb.encuentra.core.presentation.PhotoPickerLauncher
import bo.edu.ucb.encuentra.core.presentation.RegisterStepper
import bo.edu.ucb.encuentra.core.presentation.rememberCamera
import bo.edu.ucb.encuentra.core.presentation.rememberPhotoPicker
import bo.edu.ucb.encuentra.core.util.PhotoFolder
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureEffect
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureEvent
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureState
import bo.edu.ucb.encuentra.feature.foundcapture.presentation.viewmodel.FoundCaptureViewModel
import coil3.compose.AsyncImage
import edu.ucb.ds.components.buttons.AppButtonSize
import edu.ucb.ds.components.buttons.PrimaryButton
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppHeading
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.label.AppTitle
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme
import org.koin.compose.viewmodel.koinViewModel

/** Figma "Registrar – Captura de Foto" (H01): paso 1 del registro de un objeto encontrado. */
@Composable
fun FoundCaptureScreen(
    viewModel: FoundCaptureViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::handleEvent
    val camera = rememberCamera(
        folder = PhotoFolder.FOUND_ITEMS,
        onCaptured = { onEvent(FoundCaptureEvent.OnPhotoAdded(it)) },
        onPermission = { onEvent(FoundCaptureEvent.OnPermissionResult(it)) }
    )
    val gallery = rememberPhotoPicker(PhotoFolder.FOUND_ITEMS) { onEvent(FoundCaptureEvent.OnPhotoAdded(it)) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FoundCaptureEffect.NavigateToForm -> onContinue()
                FoundCaptureEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    CaptureContent(state, camera, gallery, onEvent)
    state.permission?.let { PermissionDialog(it, camera, onEvent) }
}

@Composable
private fun CaptureContent(state: FoundCaptureState, camera: CameraLauncher, gallery: PhotoPickerLauncher, onEvent: (FoundCaptureEvent) -> Unit) {
    val spacing = UcbEncuentraTheme.spacing
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // El mockup dice "Reportar": se corrige a "Registrar" (esto lo hace el staff, no el estudiante).
            CurvedHeader(
                title = "Registrar",
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = "Cancelar registro",
                onNavigationClick = { onEvent(FoundCaptureEvent.OnCancel) }
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(spacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                RegisterStepper(activeSteps = setOf(1))
                AppHeading("Evidencia fotográfica")
                AppBody("Toma al menos una foto clara del objeto encontrado. Múltiples ángulos ayudan a una mejor identificación.")
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
                    if (camera.isAvailable) {
                        SourceTile("Tomar\nFoto", Icons.Outlined.PhotoCamera, highlighted = true, enabled = state.canAddPhoto, Modifier.weight(1f)) {
                            camera.launch()
                        }
                    }
                    if (gallery.isAvailable) {
                        SourceTile("Subir\nGalería", Icons.Outlined.Image, highlighted = false, enabled = state.canAddPhoto, Modifier.weight(1f)) {
                            gallery.launch()
                        }
                    }
                }
                CapturedPhotos(state, onAdd = { if (camera.isAvailable) camera.launch() else gallery.launch() }, onEvent = onEvent)
            }
            Box(Modifier.padding(spacing.screenPadding)) {
                PrimaryButton(
                    text = "Continuar",
                    onClick = { onEvent(FoundCaptureEvent.OnContinue) },
                    enabled = state.canContinue,
                    size = AppButtonSize.Large,
                    trailingIcon = DsIcons.ArrowForward,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** "Tomar Foto" (relleno, como el mockup) y "Subir Galería" (claro). */
@Composable
private fun SourceTile(text: String, icon: ImageVector, highlighted: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    Column(
        modifier
            .clip(UcbEncuentraTheme.shapes.extraLarge)
            .background(if (highlighted) colors.surface else colors.background)
            .border(1.dp, colors.outlineVariant, UcbEncuentraTheme.shapes.extraLarge)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(56.dp).background(if (highlighted) colors.primary else colors.secondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (highlighted) colors.onPrimary else colors.onSecondary)
        }
        AppTitle(text, textAlign = TextAlign.Center, color = if (enabled) colors.onSurface else colors.outline)
    }
}

/** "Fotos capturadas (N)": miniaturas con X para eliminar y "+ Agregar". */
@Composable
private fun CapturedPhotos(state: FoundCaptureState, onAdd: () -> Unit, onEvent: (FoundCaptureEvent) -> Unit) {
    val colors = UcbEncuentraTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.surface, UcbEncuentraTheme.shapes.extraLarge).padding(UcbEncuentraTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)
    ) {
        AppOverline("Fotos capturadas (${state.photos.size})")
        if (state.photos.isEmpty()) {
            AppBody("Todavía no hay fotos. Se necesita al menos una para continuar.")
        }
        // Filas de 3 miniaturas.
        (state.photos + if (state.canAddPhoto && state.photos.isNotEmpty()) listOf(ADD) else emptyList()).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.itemGap)) {
                row.forEach { path ->
                    if (path == ADD) AddTile(onAdd) else Thumbnail(path) { onEvent(FoundCaptureEvent.OnRemovePhoto(path)) }
                }
            }
        }
        if (!state.canAddPhoto) AppLabel("Máximo ${FoundCaptureState.MAX_PHOTOS} fotos.")
    }
}

private const val ADD = "__agregar__"

@Composable
private fun Thumbnail(path: String, onRemove: () -> Unit) {
    Box(Modifier.size(88.dp)) {
        AsyncImage(
            model = path,
            contentDescription = "Foto capturada",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(UcbEncuentraTheme.shapes.medium)
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Eliminar foto", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val colors = UcbEncuentraTheme.colors
    Column(
        Modifier.size(88.dp).clip(UcbEncuentraTheme.shapes.medium)
            .border(1.dp, colors.outline, UcbEncuentraTheme.shapes.medium)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(DsIcons.Add, contentDescription = null, tint = colors.onSurfaceVariant)
        AppLabel("Agregar")
    }
}

/** Permiso CAMERA: el diálogo de justificación común (F11) antes de volver a pedirlo o de mandar a Ajustes. */
@Composable
private fun PermissionDialog(result: CameraPermissionResult, camera: CameraLauncher, onEvent: (FoundCaptureEvent) -> Unit) {
    val dismiss = { onEvent(FoundCaptureEvent.OnDismissPermission) }
    val permanentlyDenied = result == CameraPermissionResult.PERMANENTLY_DENIED
    PermissionRationaleDialog(
        permission = AppPermission.CAMERA,
        permanentlyDenied = permanentlyDenied,
        onContinue = {
            dismiss()
            if (permanentlyDenied) camera.openSettings() else camera.requestPermission()
        },
        onDismiss = dismiss
    )
}
