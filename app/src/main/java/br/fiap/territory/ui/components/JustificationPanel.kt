package br.fiap.territory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.fiap.territory.ui.theme.Spacing

/** Mostra por que o alerta existe: variáveis, regra, período, fonte, confiança e atualização. */
@Composable
fun JustificationPanel(
    variables: List<Pair<String, String>>,
    rule: String,
    period: String,
    source: String,
    confidence: Float,
    confidenceText: String,
    updatedText: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text("Por que este alerta?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            variables.forEach { (label, value) ->
                Column {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Labeled("Regra aplicada", rule)
            Labeled("Período analisado", period)
            Labeled("Fonte dos dados", source)
            Labeled("Atualização", updatedText)
            Text("Índice de confiança: $confidenceText", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            LinearProgressIndicator(
                progress = { confidence.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Índice de confiança $confidenceText" },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun Labeled(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
