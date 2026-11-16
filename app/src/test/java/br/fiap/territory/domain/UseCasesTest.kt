package br.fiap.territory.domain

import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DecisionAction
import br.fiap.territory.domain.usecase.NotifyCriticalAlertsUseCase
import br.fiap.territory.domain.usecase.RegisterDecisionUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UseCasesTest {

    @Test fun registraDecisaoComResponsavelEData() = runTest {
        val decisions = FakeDecisionRepository()
        val prefs = FakePreferences()
        val useCase = RegisterDecisionUseCase(decisions, prefs, FakeTime(NOW))
        val result = useCase("a1", DecisionAction.ACATAR, "  Maria Souza ", " reforçar monitoramento ")
        assertTrue(result.isSuccess)
        val saved = decisions.decisions.value.single()
        assertEquals("Maria Souza", saved.responsible)
        assertEquals("reforçar monitoramento", saved.note)
        assertEquals(NOW, saved.timestamp)
        assertEquals("Maria Souza", prefs.lastResponsible.value)
    }

    @Test fun recusaDecisaoSemAcaoOuSemResponsavel() = runTest {
        val decisions = FakeDecisionRepository()
        val useCase = RegisterDecisionUseCase(decisions, FakePreferences(), FakeTime())
        assertTrue(useCase("a1", null, "Maria", "").isFailure)
        assertTrue(useCase("a1", DecisionAction.ADIAR, " ", "").isFailure)
        assertTrue(decisions.decisions.value.isEmpty())
    }

    @Test fun notificaAlertaCriticoUmaUnicaVez() = runTest {
        val alerts = FakeAlertRepository()
        val notifier = FakeNotifier()
        val useCase = NotifyCriticalAlertsUseCase(alerts, notifier, FakePreferences())
        val critical = sampleAlert("c1", AlertLevel.CRITICO)
        assertEquals(1, useCase(listOf(critical)))
        assertEquals(0, useCase(listOf(critical))) // mesma ocorrência: sem duplicar
        assertEquals(listOf("c1"), notifier.shown)
    }

    @Test fun naoNotificaNiveisMenores() = runTest {
        val notifier = FakeNotifier()
        val useCase = NotifyCriticalAlertsUseCase(FakeAlertRepository(), notifier, FakePreferences())
        useCase(listOf(sampleAlert("w", AlertLevel.ATENCAO), sampleAlert("i", AlertLevel.INFORMATIVO)))
        assertTrue(notifier.shown.isEmpty())
    }

    @Test fun naoMarcaComoNotificadoQuandoPermissaoNegada() = runTest {
        val alerts = FakeAlertRepository()
        val useCase = NotifyCriticalAlertsUseCase(alerts, FakeNotifier(delivers = false), FakePreferences())
        assertEquals(0, useCase(listOf(sampleAlert("c1"))))
        assertTrue("c1" !in alerts.notified) // poderá avisar quando a permissão for concedida
    }

    @Test fun respeitaPreferenciaDeNotificacaoDesligada() = runTest {
        val notifier = FakeNotifier()
        val useCase = NotifyCriticalAlertsUseCase(FakeAlertRepository(), notifier, FakePreferences(notifications = false))
        assertEquals(0, useCase(listOf(sampleAlert("c1"))))
        assertTrue(notifier.shown.isEmpty())
    }

    @Test fun decisaoIdenticaEmSequenciaEBloqueadaMasDiferenteOuTardiaEPermitida() = runTest {
        val decisions = FakeDecisionRepository()
        val time = FakeTime(NOW)
        val useCase = RegisterDecisionUseCase(decisions, FakePreferences(), time)
        assertTrue(useCase("a1", DecisionAction.ACATAR, "Maria", "ok").isSuccess)
        assertTrue(useCase("a1", DecisionAction.ACATAR, "Maria", "ok").isFailure) // toque duplo
        assertEquals(1, decisions.decisions.value.size)
        assertTrue(useCase("a1", DecisionAction.ADIAR, "Maria", "ok").isSuccess) // ação diferente
        time.current = NOW + 31_000
        assertTrue(useCase("a1", DecisionAction.ADIAR, "Maria", "ok").isSuccess) // fora da janela de 30 s
        assertEquals(3, decisions.decisions.value.size)
    }

    @Test fun decisaoFicaVinculadaAoAlertaCorreto() = runTest {
        val decisions = FakeDecisionRepository()
        val useCase = RegisterDecisionUseCase(decisions, FakePreferences(), FakeTime())
        useCase("a1", DecisionAction.ACATAR, "Maria", "")
        useCase("a2", DecisionAction.DESCARTAR, "João", "falso positivo")
        assertEquals(listOf("a1"), decisions.decisions.value.filter { it.responsible == "Maria" }.map { it.alertId })
        assertEquals(listOf("a2"), decisions.decisions.value.filter { it.action == DecisionAction.DESCARTAR }.map { it.alertId })
    }

    @Test fun notificacaoTrazOResumoDoAlertaNoCorpo() = runTest {
        val notifier = FakeNotifier()
        NotifyCriticalAlertsUseCase(FakeAlertRepository(), notifier, FakePreferences())(listOf(sampleAlert("c9")))
        assertTrue(notifier.bodies.single().contains("resumo"))
        assertTrue(notifier.bodies.single().contains("decisão é sua"))
    }
}
