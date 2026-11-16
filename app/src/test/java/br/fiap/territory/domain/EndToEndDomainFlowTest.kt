package br.fiap.territory.domain

import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.model.IndicatorState
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.prediction.PredictionEngine
import br.fiap.territory.domain.usecase.ObserveAlertDetailUseCase
import br.fiap.territory.domain.usecase.ObserveHistoryUseCase
import br.fiap.territory.domain.usecase.ObserveTerritoryEvaluationUseCase
import br.fiap.territory.domain.usecase.RegisterDecisionUseCase
import br.fiap.territory.domain.usecase.TerritoryEvaluator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integração do domínio com os dados reais do seed (sem Android):
 * CSV → leituras → avaliação → alerta persistido → justificativa → decisão humana → histórico.
 */
class EndToEndDomainFlowTest {
    private val alerts = FakeAlertRepository()
    private val decisions = FakeDecisionRepository()
    private val prefs = FakePreferences()
    private val time = FakeTime(System.currentTimeMillis())

    private val observeEvaluation = ObserveTerritoryEvaluationUseCase(
        FakeTerritoryRepository(SeedFiles.territories()),
        FakeIndicatorRepository(SeedFiles.indicators()),
        FakeReadingRepository(SeedFiles.readings()),
        alerts,
        TerritoryEvaluator(PredictionEngine()),
        time,
    )

    @Test fun parqueCentralGeraAlertaCriticoDeEnergiaComJustificativaCompleta() = runTest {
        val evaluation = observeEvaluation("bairro-c", demoMode = true).first()
        val alert = evaluation.alerts.first { it.indicator == IndicatorType.ENERGY }
        assertEquals(AlertLevel.CRITICO, alert.level)
        assertTrue(alert.confidence >= 0.5)
        assertTrue(alert.justification.variables.size >= 6)
        assertTrue(alert.justification.source.isNotBlank() && alert.justification.period.isNotBlank())
        assertTrue(alerts.alerts.value.any { it.id == alert.id }) // persistido
    }

    @Test fun territorioNormalNaoGeraAlertaMasMostraIndicadoresOk() = runTest {
        val evaluation = observeEvaluation("bairro-a", demoMode = true).first()
        assertTrue(evaluation.alerts.isEmpty())
        assertTrue(evaluation.statuses.all { it.state == IndicatorState.OK })
    }

    @Test fun semModoDemonstracaoDadosAntigosSaoMarcadosComoDesatualizados() = runTest {
        // relógio muito depois da última leitura do seed: nenhum alerta, estado STALE
        time.current = SeedFiles.readings().maxOf { it.timestamp } + 60 * DAY
        val evaluation = observeEvaluation("bairro-c", demoMode = false).first()
        assertTrue(evaluation.alerts.isEmpty())
        assertTrue(evaluation.statuses.all { it.state == IndicatorState.STALE })
    }

    @Test fun fluxoCompletoAlertaJustificativaDecisaoHistorico() = runTest {
        val alert = observeEvaluation("bairro-c", demoMode = true).first().alerts.first()

        val detailBefore = ObserveAlertDetailUseCase(alerts, decisions)(alert.id).first()
        assertNotNull(detailBefore)
        assertTrue(detailBefore!!.decisions.isEmpty())

        val result = RegisterDecisionUseCase(decisions, prefs, time)(alert.id, DecisionAction.ACATAR, "Maria Souza", "reforçar monitoramento")
        assertTrue(result.isSuccess)

        val detailAfter = ObserveAlertDetailUseCase(alerts, decisions)(alert.id).first()!!
        assertEquals(DecisionAction.ACATAR, detailAfter.decisions.single().action)

        val history = ObserveHistoryUseCase(alerts, decisions)().first()
        assertEquals(alert.title, history.single().alertTitle)
        assertEquals("Maria Souza", history.single().decision.responsible)
        assertEquals(AlertLevel.CRITICO, history.single().alertLevel)
    }
}
