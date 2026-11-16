package br.fiap.territory.domain

import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.IndicatorState
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.prediction.PredictionEngine
import br.fiap.territory.domain.usecase.TerritoryEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerritoryEvaluatorTest {
    private val evaluator = TerritoryEvaluator(PredictionEngine())
    private val territory = Territory("t1", "Bairro Teste", "bairro fictício")
    private val indicators = listOf(energyIndicator, waterIndicator)

    private fun evaluate(energy: List<Double>, nowMillis: Long = NOW) = evaluator.evaluate(
        territory, indicators,
        mapOf(IndicatorType.ENERGY to dailyReadings(IndicatorType.ENERGY, energy)),
        nowMillis, demoMode = false,
    )

    @Test fun projecaoAcimaDoLimiarCriticoGeraAlertaCriticoComJustificativa() {
        val result = evaluate(List(14) { 9.0 + it * 0.25 }) // termina em 12,25 (>= 11,5)
        val alert = result.alerts.single()
        assertEquals(AlertLevel.CRITICO, alert.level)
        assertTrue(alert.justification.variables.isNotEmpty())
        assertTrue(alert.justification.rule.contains("regressão linear"))
        assertTrue(alert.justification.source.contains("Sintética"))
        assertTrue(alert.confidence > 0.5)
        assertTrue(alert.id.startsWith("ENERGY_t1_CRITICO_"))
    }

    @Test fun entreOsLimiaresGeraAtencao() {
        val result = evaluate(List(14) { 10.5 })
        assertEquals(AlertLevel.ATENCAO, result.alerts.single().level)
    }

    @Test fun altaAindaAbaixoDoLimiarGeraInformativo() {
        val result = evaluate(List(14) { 8.5 + it * 0.06 }) // projeção ~9,34: >= 90% do limiar de atenção (9,0) e < 10,0
        assertEquals(AlertLevel.INFORMATIVO, result.alerts.single().level)
    }

    @Test fun serieNormalNaoGeraAlerta() {
        val result = evaluate(List(14) { 7.0 })
        assertTrue(result.alerts.isEmpty())
        assertNull(result.statuses.first { it.indicator.type == IndicatorType.ENERGY }.level)
    }

    @Test fun poucosDadosNaoEmitemAlertaEExplicamOMotivo() {
        val result = evaluate(List(3) { 20.0 })
        assertTrue(result.alerts.isEmpty())
        val status = result.statuses.first { it.indicator.type == IndicatorType.ENERGY }
        assertEquals(IndicatorState.INSUFFICIENT, status.state)
        assertTrue(status.message!!.contains("insuficientes"))
    }

    @Test fun dadosVencidosNaoEmitemAlerta() {
        val result = evaluate(List(14) { 20.0 }, nowMillis = NOW + 10 * DAY)
        assertTrue(result.alerts.isEmpty())
        assertEquals(IndicatorState.STALE, result.statuses.first { it.indicator.type == IndicatorType.ENERGY }.state)
    }

    @Test fun confiancaBaixaNaoEmiteAlerta() {
        val result = evaluate(List(14) { if (it % 2 == 0) 6.0 else 20.0 })
        assertTrue(result.alerts.isEmpty())
        assertEquals(IndicatorState.LOW_CONFIDENCE, result.statuses.first { it.indicator.type == IndicatorType.ENERGY }.state)
    }

    @Test fun semLeituraDeAguaMarcaSemDados() {
        val result = evaluate(List(14) { 7.0 })
        assertEquals(IndicatorState.NO_DATA, result.statuses.first { it.indicator.type == IndicatorType.WATER }.state)
    }

    @Test fun temperaturaPrevistaEntraNaProjecao() {
        val climate = dailyReadings(IndicatorType.CLIMATE, listOf(34.0), endMillis = NOW + DAY)
        val result = evaluator.evaluate(
            territory, indicators,
            mapOf(
                IndicatorType.ENERGY to dailyReadings(IndicatorType.ENERGY, List(14) { 9.8 }),
                IndicatorType.CLIMATE to climate,
            ),
            NOW, demoMode = false,
        )
        // 9,8 x (1 + 1,5% x 4) = 10,388 -> acima do limiar de atenção (10,0)
        assertEquals(AlertLevel.ATENCAO, result.alerts.single().level)
        assertEquals(34.0, result.forecastTemperatureC!!, 1e-9)
    }

    @Test fun limiaresSaoInclusivosNoValorExato() {
        assertEquals(AlertLevel.ATENCAO, evaluate(List(14) { 10.0 }).alerts.single().level) // = atenção
        assertEquals(AlertLevel.CRITICO, evaluate(List(14) { 11.5 }).alerts.single().level) // = crítico
    }

    @Test fun logoAbaixoDoLimiarDeAtencaoEstavelNaoAlerta() {
        assertTrue(evaluate(List(14) { 9.99 }).alerts.isEmpty())
    }

    @Test fun leituraFuturaDeEnergiaNaoGeraAlerta() {
        val past = dailyReadings(IndicatorType.ENERGY, List(14) { 7.0 })
        val spike = dailyReadings(IndicatorType.ENERGY, listOf(50.0), endMillis = NOW + DAY)
        val result = evaluator.evaluate(
            territory, indicators, mapOf(IndicatorType.ENERGY to past + spike), NOW, demoMode = false,
        )
        assertTrue(result.alerts.isEmpty())
    }

    @Test fun cadaAlertaTemIdDeterministicoPorDiaParaNaoDuplicar() {
        val a = evaluate(List(14) { 11.5 }).alerts.single()
        val b = evaluate(List(14) { 11.5 }).alerts.single()
        assertEquals(a.id, b.id)
    }
}
