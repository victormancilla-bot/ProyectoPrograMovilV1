package bo.edu.ucb.encuentra.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Pantalla temporal de F01. Cada dueño la reemplaza por su pantalla real. */
@Composable
fun FeatureStub(
    title: String,
    owner: String,
    card: String,
    detail: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("En construcción · $owner · $card", style = MaterialTheme.typography.bodyMedium)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))
        actions()
        OutlinedButton(onClick = onNavigateBack) {
            Text("Volver")
        }
    }
}
