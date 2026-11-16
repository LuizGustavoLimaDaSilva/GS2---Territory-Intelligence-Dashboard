package br.fiap.territory.ui

import app.cash.turbine.test
import br.fiap.territory.domain.FakeAlertRepository
import br.fiap.territory.domain.FakeDecisionRepository
import br.fiap.territory.domain.NOW
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.sampleAlert
import br.fiap.territory.domain.usecase.ObserveHistoryUseCase
import br.fiap.territory.ui.history.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun semDecisoesMostraVazioEDepoisSucessoComTituloDoAlerta() = runTest {
        val alerts = FakeAlertRepository().apply { upsertAll(listOf(sampleAlert("a1"))) }
        val decisions = FakeDecisionRepository()
        val viewModel = HistoryViewModel(ObserveHistoryUseCase(alerts, decisions))

        viewModel.state.test {
            var item = awaitItem()
            if (item is UiState.Loading) item = awaitItem()
            assertTrue("esperava Empty, veio $item", item is UiState.Empty)

            decisions.insert(Decision("d1", "a1", "Maria", DecisionAction.ACATAR, "ok", NOW))
            val success = awaitItem() as UiState.Success
            assertEquals("Alerta a1", success.data.single().alertTitle)
            assertEquals(DecisionAction.ACATAR, success.data.single().decision.action)

            decisions.clearAll()
            assertTrue(awaitItem() is UiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
