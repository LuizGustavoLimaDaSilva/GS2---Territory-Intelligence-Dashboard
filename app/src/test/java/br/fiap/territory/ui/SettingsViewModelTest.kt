package br.fiap.territory.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import br.fiap.territory.domain.FakeDecisionRepository
import br.fiap.territory.domain.FakeIndicatorRepository
import br.fiap.territory.domain.FakeNotifier
import br.fiap.territory.domain.FakePreferences
import br.fiap.territory.domain.FakeTerritoryRepository
import br.fiap.territory.domain.energyIndicator
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.NOW
import br.fiap.territory.domain.usecase.ClearDecisionsUseCase
import br.fiap.territory.ui.settings.SettingsData
import br.fiap.territory.ui.settings.SettingsViewModel
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
class SettingsViewModelTest {
    private val territory = Territory("t1", "Bairro Teste", "bairro fictício")
    private val decisions = FakeDecisionRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(territories: FakeTerritoryRepository, prefs: FakePreferences = FakePreferences()) =
        SettingsViewModel(territories, FakeIndicatorRepository(listOf(energyIndicator)), prefs, ClearDecisionsUseCase(decisions), FakeNotifier())

    private suspend fun <T> ReceiveTurbine<T>.awaitMatching(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test fun carregaConfiguracoesComTerritorioSelecionadoPadrao() = runTest {
        val vm = viewModel(FakeTerritoryRepository(listOf(territory)))
        vm.state.test {
            val success = awaitMatching { it is UiState.Success<*> } as UiState.Success<SettingsData>
            assertEquals("t1", success.data.selectedId)
            assertEquals(1, success.data.indicators.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun falhaDeLeituraViraErroENaoSucessoComListaVazia() = runTest {
        val vm = viewModel(FakeTerritoryRepository(listOf(territory), failing = true))
        vm.state.test {
            val state = awaitMatching { it !is UiState.Loading }
            assertTrue("esperava Error, veio $state", state is UiState.Error)
            assertTrue((state as UiState.Error).message.contains("Não foi possível ler"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun retryRecuperaDepoisDaFalha() = runTest {
        val territories = FakeTerritoryRepository(listOf(territory), failing = true)
        val vm = viewModel(territories)
        vm.state.test {
            awaitMatching { it is UiState.Error }
            territories.failing = false
            vm.retry()
            assertTrue(awaitMatching { it is UiState.Success<*> } is UiState.Success<*>)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun listaVaziaLegitimaEDiferenciadaDeFalha() = runTest {
        val vm = viewModel(FakeTerritoryRepository(emptyList()))
        vm.state.test {
            val state = awaitMatching { it !is UiState.Loading }
            assertTrue("esperava Empty, veio $state", state is UiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun apagarHistoricoRemoveAsDecisoes() = runTest {
        decisions.insert(Decision("d1", "a1", "Maria", DecisionAction.ACATAR, "", NOW))
        val vm = viewModel(FakeTerritoryRepository(listOf(territory)))
        vm.deleteDecisions()
        assertTrue(decisions.decisions.value.isEmpty())
    }

    @Test fun preferenciasSaoGravadas() = runTest {
        val prefs = FakePreferences()
        val vm = viewModel(FakeTerritoryRepository(listOf(territory)), prefs)
        vm.setDemoMode(true)
        vm.setNotificationsEnabled(false)
        vm.selectTerritory("t1")
        assertTrue(prefs.demoMode.value)
        assertEquals(false, prefs.notificationsEnabled.value)
        assertEquals("t1", prefs.selectedTerritoryId.value)
    }
}
