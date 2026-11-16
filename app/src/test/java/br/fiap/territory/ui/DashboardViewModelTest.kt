package br.fiap.territory.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import br.fiap.territory.domain.DAY
import br.fiap.territory.domain.FakeAlertRepository
import br.fiap.territory.domain.FakeIndicatorRepository
import br.fiap.territory.domain.FakeNotifier
import br.fiap.territory.domain.FakePreferences
import br.fiap.territory.domain.FakeReadingRepository
import br.fiap.territory.domain.FakeSeeder
import br.fiap.territory.domain.FakeTerritoryRepository
import br.fiap.territory.domain.FakeTime
import br.fiap.territory.domain.NOW
import br.fiap.territory.domain.dailyReadings
import br.fiap.territory.domain.energyIndicator
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.SeedStatus
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.prediction.PredictionEngine
import br.fiap.territory.domain.usecase.NotifyCriticalAlertsUseCase
import br.fiap.territory.domain.usecase.ObserveTerritoryEvaluationUseCase
import br.fiap.territory.domain.usecase.TerritoryEvaluator
import br.fiap.territory.domain.waterIndicator
import br.fiap.territory.ui.dashboard.DashboardData
import br.fiap.territory.ui.dashboard.DashboardViewModel
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
class DashboardViewModelTest {
    private val territory = Territory("t1", "Bairro Teste", "bairro fictício")
    private val time = FakeTime(NOW)
    private val alerts = FakeAlertRepository()
    private val notifier = FakeNotifier()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        seeder: FakeSeeder = FakeSeeder(),
        territories: FakeTerritoryRepository = FakeTerritoryRepository(listOf(territory)),
        readings: FakeReadingRepository = FakeReadingRepository(
            dailyReadings(IndicatorType.ENERGY, List(14) { 12.0 }) + dailyReadings(IndicatorType.WATER, List(14) { 120.0 }),
        ),
        prefs: FakePreferences = FakePreferences(),
    ): DashboardViewModel {
        val observe = ObserveTerritoryEvaluationUseCase(
            territories, FakeIndicatorRepository(listOf(energyIndicator, waterIndicator)), readings,
            alerts, TerritoryEvaluator(PredictionEngine()), time,
        )
        return DashboardViewModel(
            territories, prefs, seeder, observe, NotifyCriticalAlertsUseCase(alerts, notifier, prefs), notifier, time,
        )
    }

    private suspend fun <T> ReceiveTurbine<T>.awaitMatching(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test fun carregaComSucessoMostraAlertaCriticoENotificaUmaVez() = runTest {
        val vm = viewModel()
        vm.state.test {
            val success = awaitMatching { it is UiState.Success<*> || it is UiState.Stale<*> || it is UiState.Error } as UiState.Success<DashboardData>
            assertEquals(AlertLevel.CRITICO, success.data.evaluation.alerts.single().level)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, notifier.shown.size) // alerta crítico notificado uma única vez
    }

    @Test fun retryReexecutaOSeedQuandoOCarregamentoInicialFalhouERecupera() = runTest {
        val seeder = FakeSeeder(initial = SeedStatus.Failed("falha simulada"))
        val vm = viewModel(seeder = seeder)
        vm.state.test {
            val error = awaitMatching { it is UiState.Error }
            assertTrue((error as UiState.Error).message.contains("falha simulada"))

            vm.retry() // seed volta a rodar (agora com sucesso) e a coleta é reiniciada

            awaitMatching { it is UiState.Success<*> }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, seeder.calls)
    }

    @Test fun retryContinuaNoErroSeOSeedFalharDeNovoEsemLoopInfinito() = runTest {
        val seeder = FakeSeeder(initial = SeedStatus.Failed("falha 1"), failNext = true)
        val vm = viewModel(seeder = seeder)
        vm.state.test {
            awaitMatching { it is UiState.Error }
            vm.retry()
            val again = awaitMatching { it is UiState.Error }
            assertTrue((again as UiState.Error).message.contains("falha simulada"))
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, seeder.calls) // uma tentativa por toque, sem repetição automática
    }

    @Test fun falhaDeLeituraDosTerritoriosViraErroERetryRecupera() = runTest {
        val territories = FakeTerritoryRepository(listOf(territory), failing = true)
        val vm = viewModel(territories = territories)
        vm.state.test {
            val error = awaitMatching { it is UiState.Error } as UiState.Error
            assertTrue(error.message.contains("Falha ao ler"))
            territories.failing = false
            vm.retry()
            awaitMatching { it is UiState.Success<*> }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun falhaDeLeituraDasLeiturasViraErro() = runTest {
        val vm = viewModel(readings = FakeReadingRepository(emptyList(), failing = true))
        vm.state.test {
            assertTrue(awaitMatching { it is UiState.Error } is UiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun dadosVencidosViramEstadoStaleSemAlertasENadaEhNotificado() = runTest {
        time.current = NOW + 10 * DAY
        val vm = viewModel()
        vm.state.test {
            val stale = awaitMatching { it is UiState.Stale<*> } as UiState.Stale<DashboardData>
            assertTrue(stale.data.evaluation.alerts.isEmpty())
            assertTrue(stale.message.contains("desatualizados"))
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(notifier.shown.isEmpty())
    }

    @Test fun modoDemonstracaoTrataAUltimaLeituraComoAgoraEsaiDoEstadoStale() = runTest {
        time.current = NOW + 10 * DAY
        val prefs = FakePreferences().apply { demoMode.value = true }
        val vm = viewModel(prefs = prefs)
        vm.state.test {
            val success = awaitMatching { it is UiState.Success<*> } as UiState.Success<DashboardData>
            assertTrue(success.data.evaluation.demoMode)
            assertEquals(1, success.data.evaluation.alerts.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun semTerritoriosMostraVazio() = runTest {
        val vm = viewModel(territories = FakeTerritoryRepository(emptyList()))
        vm.state.test {
            assertTrue(awaitMatching { it is UiState.Empty } is UiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun refreshReavaliaUmaVezEIgnoraChamadasNoMesmoMinuto() = runTest {
        val readings = FakeReadingRepository(
            dailyReadings(IndicatorType.ENERGY, List(14) { 12.0 }) + dailyReadings(IndicatorType.WATER, List(14) { 120.0 }),
        )
        val seeder = FakeSeeder()
        val vm = viewModel(seeder = seeder, readings = readings)
        vm.state.test {
            awaitMatching { it is UiState.Success<*> }
            val before = readings.observeCalls // 3 assinaturas por avaliação (energia, água, clima)
            vm.refresh() // aceita: reavalia
            assertEquals(before + 3, readings.observeCalls)
            vm.refresh() // no mesmo minuto: ignorada
            assertEquals(before + 3, readings.observeCalls)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, seeder.calls) // refresh não refaz o seed
    }
}
