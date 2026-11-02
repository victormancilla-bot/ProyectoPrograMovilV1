package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import edu.ucb.ds.components.buttons.SecondaryButton
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.label.AppBody
import edu.ucb.ds.components.label.AppLabel
import edu.ucb.ds.components.surfaces.AppSurface
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

/** Pantalla temporal de F01 (con el header del Design System). Cada dueño la reemplaza por su pantalla real. */
@Composable
fun FeatureStub(
    title: String,
    owner: String,
    card: String,
    detail: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
    backLabel: String = "Volver",
    onNavigateBack: () -> Unit
) {
    AppSurface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CurvedHeader(
                title = title,
                navigationIcon = DsIcons.ArrowBack,
                navigationContentDescription = backLabel,
                onNavigationClick = onNavigateBack,
                centerTitle = true
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(UcbEncuentraTheme.spacing.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(UcbEncuentraTheme.spacing.large, Alignment.CenterVertically)
            ) {
                AppBody("En construcción · $owner · $card", textAlign = TextAlign.Center)
                if (detail != null) AppLabel(detail)
                actions()
                SecondaryButton(text = backLabel, onClick = onNavigateBack)
            }
        }
    }
}
