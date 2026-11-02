package bo.edu.ucb.encuentra.feature.designsystem.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import edu.ucb.ds.components.badges.ScoreBadge
import edu.ucb.ds.components.badges.StatusChip
import edu.ucb.ds.components.badges.StatusTone
import edu.ucb.ds.components.cards.AppCardVariant
import edu.ucb.ds.components.cards.CategoryGrid
import edu.ucb.ds.components.cards.CategoryOption
import edu.ucb.ds.components.cards.ItemCard
import edu.ucb.ds.components.cards.StatCard
import edu.ucb.ds.components.header.CurvedHeader
import edu.ucb.ds.components.header.HeaderAction
import edu.ucb.ds.components.label.AppOverline
import edu.ucb.ds.components.progress.StepProgressBar
import edu.ucb.ds.components.states.EmptyState
import edu.ucb.ds.components.states.ErrorState
import edu.ucb.ds.components.states.LoadingState
import edu.ucb.ds.icons.DsIcons
import edu.ucb.ds.theme.UcbEncuentraTheme

private val demoCategories = listOf(
    CategoryOption("electronica", "Electrónica", Icons.Filled.Devices),
    CategoryOption("ropa", "Ropa/Accesorios", Icons.Filled.Checkroom),
    CategoryOption("documentos", "Documentos", Icons.Filled.Description),
    CategoryOption("libros", "Libros/Cuadernos", Icons.Filled.MenuBook),
    CategoryOption("llaves", "Llaves", Icons.Filled.Key),
    CategoryOption("otros", "Otros", Icons.Filled.MoreHoriz)
)

/** Componentes de dominio (F04) funcionando, para revisarlos contra el Figma. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DomainComponentsSection() {
    val spacing = UcbEncuentraTheme.spacing
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        AppOverline("Componentes de dominio (F04)")

        AppOverline("CurvedHeader")
        CurvedHeader(
            title = "¡Hola, Mateo!",
            subtitle = "Ingeniería • Sede La Paz",
            navigationIcon = DsIcons.Menu,
            actions = { HeaderAction(DsIcons.DarkMode, "Modo oscuro", onClick = {}) },
            modifier = Modifier.clip(UcbEncuentraTheme.shapes.large)
        )
        CurvedHeader(
            title = "Reportar objeto",
            navigationIcon = DsIcons.ArrowBack,
            centerTitle = true,
            bottomContent = { StepProgressBar(currentStep = 1, totalSteps = 4, stepName = "Categoría", onDark = true) },
            modifier = Modifier.clip(UcbEncuentraTheme.shapes.large)
        )

        AppOverline("ScoreBadge y StatusChip")
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
            ScoreBadge(92)
            ScoreBadge(65)
            ScoreBadge(30)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            StatusChip("Buscando", StatusTone.Neutral)
            StatusChip("En verificación", StatusTone.Warning)
            StatusChip("Match confirmado", StatusTone.Success)
            StatusChip("Vencido", StatusTone.Error)
            StatusChip("Disponible", StatusTone.Info)
            StatusChip("Reclamado", StatusTone.Primary)
        }

        AppOverline("CategoryTile · ¿Qué perdiste?")
        CategoryGrid(options = demoCategories, selectedId = category, onSelect = { category = it.id })

        AppOverline("StatCard")
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
            StatCard("142", "Objetos en custodia", Icons.Filled.Inventory2, Modifier.weight(1f))
            StatCard("18", "Pendientes de entrega", Icons.Filled.Assignment, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.large)) {
            StatCard("5", "En verificación", Icons.Filled.VerifiedUser, Modifier.weight(1f))
            StatCard(
                "3",
                "Por vencer",
                Icons.Filled.Schedule,
                Modifier.weight(1f),
                valueColor = UcbEncuentraTheme.colors.error
            )
        }

        AppOverline("ItemCard")
        ItemCard(
            category = "Electrónicos",
            title = "Calculadora Científica",
            date = "Encontrado: 12 Oct 2026",
            image = { Icon(Icons.Filled.Devices, contentDescription = null, tint = UcbEncuentraTheme.colors.outline) },
            score = 92,
            variant = AppCardVariant.Filled,
            onClick = {}
        )
        ItemCard(
            category = "Material de estudio",
            title = "Cuaderno de Cálculo",
            date = "Reportado: 05 Oct 2026",
            location = "Bloque D, Aula 104",
            status = { StatusChip("Match confirmado", StatusTone.Success) },
            actionText = "Ver Detalles",
            categoryColor = UcbEncuentraTheme.colors.primary
        )
        ItemCard(
            category = "Otros",
            title = "Botella de Agua Azul",
            date = "Reportado: 01 Sep 2026",
            location = "Canchas Deportivas",
            status = { StatusChip("Vencido", StatusTone.Error) },
            actionText = "Ver Detalles",
            dimmed = true
        )

        AppOverline("Estados")
        EmptyState(
            title = "Aún no tienes reportes",
            message = "Cuando reportes un objeto perdido aparecerá aquí.",
            actionText = "Reportar objeto"
        )
        ErrorState(message = "Revisa tu conexión e intenta de nuevo.", onRetry = {})
        LoadingState(message = "Buscando coincidencias…", modifier = Modifier.padding(bottom = spacing.large))
    }
}
