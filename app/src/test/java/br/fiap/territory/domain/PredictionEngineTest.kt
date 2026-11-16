package br.fiap.territory.domain

import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.ForecastStatus
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.Trend
import br.fiap.territory.domain.prediction.PredictionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PredictionEngineTest {
    private val engine = PredictionEngine()

    private fun flat(n: Int = 14, v: Double = 10.0) = dailyReadings(IndicatorType.ENERGY, List(n) { v })

    @Test fun serieEstavelFicaEstavelEProjetaOMesmoNivel() {
        val f = engine.forecast(IndicatorType.ENERGY, flat(), null, NOW)
        assertEquals(ForecastStatus.OK, f.status)
        assertEquals(Trend.STABLE, f.trend)
        assertEquals(10.0, f.projectedValue!!, 1e-9)
        assertEquals(1.0, f.confidence, 1e-9)
    }

    @Test fun tendenciaDeAltaEProjetadaAFrente() {
        val values = List(14) { 10.0 + it * 0.5 } // +0,5 por dia
        val f = engine.forecast(IndicatorType.ENERGY, dailyReadings(IndicatorType.ENERGY, values), null, NOW)
        assertEquals(Trend.INCREASING, f.trend)
        assertEquals(0.5, f.slopePerDay!!, 1e-9)
        // últimos 3 valores: 15,5; 16,0; 16,5 -> média móvel 16,0; projeção = 16,0 + 0,5 x 2 = 17,0
        assertEquals(16.0, f.movingAverage!!, 1e-9)
        assertEquals(17.0, f.projectedValue!!, 1e-9)
    }

    @Test fun tendenciaDeQuedaEDetectada() {
        val values = List(14) { 20.0 - it * 0.5 }
        val f = engine.forecast(IndicatorType.ENERGY, dailyReadings(IndicatorType.ENERGY, values), null, NOW)
        assertEquals(Trend.DECREASING, f.trend)
    }

    @Test fun temperaturaAcimaDoLimiarAumentaAProjecao() {
        val f = engine.forecast(IndicatorType.ENERGY, flat(), 34.0, NOW)
        // 10 x 1,5% x (34 - 30) = 0,6
        assertEquals(0.6, f.temperatureAdjustment, 1e-9)
        assertEquals(10.6, f.projectedValue!!, 1e-9)
    }

    @Test fun temperaturaAbaixoDoLimiarNaoAjusta() {
        val f = engine.forecast(IndicatorType.ENERGY, flat(), 29.0, NOW)
        assertEquals(0.0, f.temperatureAdjustment, 1e-9)
    }

    @Test fun poucosDiasGeramDadosInsuficientes() {
        val f = engine.forecast(IndicatorType.ENERGY, flat(n = 3), null, NOW)
        assertEquals(ForecastStatus.INSUFFICIENT, f.status)
        assertNull(f.projectedValue)
    }

    @Test fun dadosVencidosGeramStaleEMantemUltimoValor() {
        val f = engine.forecast(IndicatorType.ENERGY, flat(), null, NOW + 8 * DAY)
        assertEquals(ForecastStatus.STALE, f.status)
        assertEquals(10.0, f.lastValue!!, 1e-9)
        assertNull(f.projectedValue)
    }

    @Test fun semLeiturasGeraNoData() {
        assertEquals(ForecastStatus.NO_DATA, engine.forecast(IndicatorType.WATER, emptyList(), null, NOW).status)
    }

    @Test fun valoresInvalidosEDuplicadosSaoIgnorados() {
        val base = flat(n = 7)
        val ruins = listOf(
            Reading("x1", "t1", IndicatorType.ENERGY, NOW, Double.NaN, DataSource.SINTETICA),
            Reading("x2", "t1", IndicatorType.ENERGY, NOW, -5.0, DataSource.SINTETICA),
        )
        val f = engine.forecast(IndicatorType.ENERGY, base + base.first() + ruins, null, NOW)
        assertEquals(7, f.samples)
    }

    @Test fun energiaUsaOPicoDoDiaEAguaUsaAMedia() {
        val hourly = listOf(5.0, 9.0, 7.0).mapIndexed { i, v ->
            Reading("h$i", "t1", IndicatorType.ENERGY, NOW - (2 - i) * 3_600_000L, v, DataSource.SINTETICA)
        }
        val energyDaily = br.fiap.territory.domain.prediction.DailyAggregator
            .aggregate(hourly, IndicatorType.ENERGY.aggregation)
        val waterDaily = br.fiap.territory.domain.prediction.DailyAggregator
            .aggregate(hourly, IndicatorType.WATER.aggregation)
        assertEquals(9.0, energyDaily.single().value, 1e-9)
        assertEquals(7.0, waterDaily.single().value, 1e-9)
    }

    @Test fun ruidoAltoReduzAConfianca() {
        val noisy = List(14) { if (it % 2 == 0) 6.0 else 14.0 }
        val f = engine.forecast(IndicatorType.ENERGY, dailyReadings(IndicatorType.ENERGY, noisy), null, NOW)
        assertTrue(f.confidence < 0.5)
    }

    @Test fun leiturasFuturasNaoEntramNoHistoricoNemNaProjecao() {
        val past = flat(n = 14, v = 10.0)
        val future = (1..3).map { k ->
            Reading("f$k", "t1", IndicatorType.ENERGY, NOW + k * DAY, 500.0, DataSource.SINTETICA)
        }
        val withFuture = engine.forecast(IndicatorType.ENERGY, past + future, null, NOW)
        val without = engine.forecast(IndicatorType.ENERGY, past, null, NOW)
        assertEquals(without.projectedValue!!, withFuture.projectedValue!!, 1e-9)
        assertEquals(10.0, withFuture.lastValue!!, 1e-9)
        assertEquals(without.history, withFuture.history)
    }

    @Test fun seTodasAsLeiturasSaoFuturasNaoHaDadosEORazaoExplicaIsso() {
        val future = dailyReadings(IndicatorType.ENERGY, List(14) { 10.0 }, endMillis = NOW + 30 * DAY)
        val f = engine.forecast(IndicatorType.ENERGY, future, null, NOW)
        assertEquals(ForecastStatus.NO_DATA, f.status)
        assertTrue(f.reason!!.contains("posteriores"))
    }

    @Test fun confiancaDependeDaCoberturaDaJanela() {
        // 10 de 14 dias, série perfeitamente estável: 10/14 x (1 - 0) = 0,714...
        val f = engine.forecast(IndicatorType.ENERGY, flat(n = 10), null, NOW)
        assertEquals(10.0 / 14.0, f.confidence, 1e-9)
    }

    @Test fun serieZeradaNaoProduzNaNNemInfinito() {
        val f = engine.forecast(IndicatorType.ENERGY, flat(v = 0.0), 35.0, NOW)
        assertEquals(ForecastStatus.OK, f.status)
        listOf(f.movingAverage!!, f.slopePerDay!!, f.projectedValue!!, f.confidence).forEach {
            assertTrue("valor não finito: $it", it.isFinite())
        }
        assertEquals(0.0, f.projectedValue!!, 1e-9)
    }

    @Test fun valoresExtremosPermanecemFinitos() {
        val huge = dailyReadings(IndicatorType.WATER, List(14) { 1e300 * (1 + it) })
        val f = engine.forecast(IndicatorType.WATER, huge, null, NOW)
        if (f.status == ForecastStatus.OK) {
            assertTrue(f.projectedValue!!.isFinite() && f.confidence.isFinite())
        } else {
            assertEquals(ForecastStatus.INSUFFICIENT, f.status) // cálculo inválido é recusado, nunca vira alerta
        }
    }

    @Test fun confiancaFicaSempreEntreZeroEUm() {
        listOf(
            List(14) { if (it % 2 == 0) 1.0 else 100.0 },
            List(14) { it * 3.0 },
            List(7) { 5.0 },
        ).forEach { values ->
            val f = engine.forecast(IndicatorType.ENERGY, dailyReadings(IndicatorType.ENERGY, values), null, NOW)
            assertTrue(f.confidence in 0.0..1.0)
        }
    }
}
