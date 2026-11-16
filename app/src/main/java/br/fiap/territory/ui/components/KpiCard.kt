package br.fiap.territory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.fiap.territory.ui.theme.Spacing

/** Cartão padrão do app: um só lugar define cor, borda e clique. */
@Composable
internal fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier, colors = colors, border = border, content = content)
    }
}

/** Selo de status com ícone + rótulo (nunca só cor). */
@Composable
fun StatusChip(label: String, status: StatusKind, modifier: Modifier = Modifier) {
    val palette = status.palette()
    Surface(shape = MaterialTheme.shapes.small, color = palette.container, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(status.icon, contentDescription = null, tint = palette.content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(label, style = MaterialTheme.typography.labelLarge, color = palette.content)
        }
    }
}

/** Indicador com valor, unidade, tendência e status. Descreve; não decide. */
@Composable
fun KpiCard(
    title: String,
    value: String,
    unit: String,
    statusLabel: String,
    status: StatusKind,
    details: List<String>,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    extraContent: (@Composable () -> Unit)? = null,
) {
    AppCard(
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.md)
                .semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(statusLabel, status)
            extraContent?.invoke()
            details.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
