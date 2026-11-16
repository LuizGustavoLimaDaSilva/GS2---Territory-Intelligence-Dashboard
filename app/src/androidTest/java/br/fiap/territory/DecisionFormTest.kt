package br.fiap.territory

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.ui.components.DecisionForm
import br.fiap.territory.ui.theme.TerritoryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DecisionFormTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmarSoHabilitaComAcaoEResponsavelEEntregaOsValores() {
        var received: Triple<DecisionAction, String, String>? = null
        compose.setContent {
            TerritoryTheme {
                DecisionForm(
                    alertTitle = "Crítico: risco de sobrecarga",
                    initialResponsible = "",
                    errorMessage = null,
                    onConfirm = { a, r, n -> received = Triple(a, r, n) },
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithText("Confirmar decisão").assertIsNotEnabled()
        compose.onNodeWithText("Adiar").performClick()
        compose.onNodeWithText("Responsável pela decisão").performTextInput("Maria Souza")
        compose.onNodeWithText("Observação (opcional)").performTextInput("rever amanhã")
        compose.onNodeWithText("Confirmar decisão").assertIsEnabled().performClick()
        assertEquals(Triple(DecisionAction.ADIAR, "Maria Souza", "rever amanhã"), received)
    }
}
