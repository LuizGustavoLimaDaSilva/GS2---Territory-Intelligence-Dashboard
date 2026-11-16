package br.fiap.territory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import br.fiap.territory.ui.theme.Spacing

enum class StateKind { LOADING, EMPTY, ERROR, STALE }

/** Padroniza carregando, vazio, erro e dados desatualizados, com ação opcional. */
@Composable
fun StateView(
    kind: StateKind,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    if (kind == StateKind.LOADING) {
        Column(
            modifier = modifier.fillMaxWidth().padding(Spacing.lg).semantics { contentDescription = message },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val status = when (kind) {
        StateKind.ERROR -> StatusKind.CRITICAL
        StateKind.STALE -> StatusKind.ATTENTION
        else -> StatusKind.INFO
    }
    val palette = status.palette()
    AppCard(modifier = modifier.fillMaxWidth(), containerColor = palette.container) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Top) {
                Icon(status.icon, contentDescription = null, tint = palette.content)
                Text(message, style = MaterialTheme.typography.bodyLarge, color = palette.content)
            }
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.heightIn(min = Spacing.minTouch)) { Text(actionLabel) }
            }
        }
    }
}
