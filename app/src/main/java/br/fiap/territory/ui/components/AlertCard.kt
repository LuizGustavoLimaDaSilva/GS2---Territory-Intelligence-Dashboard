package br.fiap.territory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.fiap.territory.ui.theme.Spacing

/** Alerta com nível (ícone + rótulo), resumo, confiança e atualização. O clique abre a justificativa. */
@Composable
fun AlertCard(
    levelLabel: String,
    level: StatusKind,
    title: String,
    summary: String,
    metaLine: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val palette = level.palette()
    AppCard(
        modifier = modifier.fillMaxWidth().heightIn(min = 96.dp),
        onClick = onClick,
        containerColor = palette.container,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(level.icon, contentDescription = null, tint = palette.content, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(levelLabel.uppercase(), style = MaterialTheme.typography.labelLarge, color = palette.content)
                Text(title, style = MaterialTheme.typography.titleMedium, color = palette.content)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = palette.content)
                Text(metaLine, style = MaterialTheme.typography.labelSmall, color = palette.content)
                if (onClick != null) {
                    Text("Toque para ver a justificativa", style = MaterialTheme.typography.labelLarge, color = palette.content)
                }
            }
        }
    }
}
