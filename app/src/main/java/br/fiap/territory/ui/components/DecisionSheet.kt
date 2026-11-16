package br.fiap.territory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.ui.theme.Spacing

/** Registro da decisão humana sobre um alerta: acatar, adiar ou descartar. */
@Composable
fun DecisionSheet(
    alertTitle: String,
    initialResponsible: String,
    errorMessage: String?,
    isSubmitting: Boolean,
    onConfirm: (DecisionAction, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        DecisionForm(
            alertTitle = alertTitle,
            initialResponsible = initialResponsible,
            errorMessage = errorMessage,
            isSubmitting = isSubmitting,
            onConfirm = onConfirm,
            onCancel = onDismiss,
        )
    }
}

/** Conteúdo do formulário (separado do sheet para poder ser testado sozinho). */
@Composable
fun DecisionForm(
    alertTitle: String,
    initialResponsible: String,
    errorMessage: String?,
    onConfirm: (DecisionAction, String, String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
) {
    var action by rememberSaveable { mutableStateOf<DecisionAction?>(null) }
    var responsible by rememberSaveable { mutableStateOf(initialResponsible) }
    var note by rememberSaveable { mutableStateOf("") }
    val canConfirm = action != null && responsible.trim().length >= 2 && !isSubmitting

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("Registrar decisão", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(alertTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "O app apenas recomenda. A decisão é sua e fica registrada com data e responsável.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.selectableGroup()) {
            DecisionAction.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.minTouch)
                        .selectable(
                            selected = action == option,
                            enabled = !isSubmitting,
                            onClick = { action = option },
                            role = Role.RadioButton,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = action == option, onClick = null)
                    Text(
                        option.label,
                        modifier = Modifier.padding(start = Spacing.sm),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        OutlinedTextField(
            value = responsible,
            onValueChange = { responsible = it },
            label = { Text("Responsável pela decisão") },
            singleLine = true,
            enabled = !isSubmitting,
            isError = responsible.isNotEmpty() && responsible.trim().length < 2,
            supportingText = { Text("Obrigatório: ao menos 2 caracteres.") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Observação (opcional)") },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorMessage != null) {
            Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { action?.let { onConfirm(it, responsible, note) } },
            enabled = canConfirm,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
        ) { Text(if (isSubmitting) "Registrando…" else "Confirmar decisão") }
        if (action == null) {
            Text(
                "Escolha uma ação (acatar, adiar ou descartar) para confirmar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onCancel,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.minTouch),
        ) { Text("Cancelar") }
    }
}
