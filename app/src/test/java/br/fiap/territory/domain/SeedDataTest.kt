package br.fiap.territory.domain

import br.fiap.territory.data.seed.parseCsvLine
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.IndicatorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Valida a consistência dos CSVs de assets/seed (a mesma base que o app carrega no Room). */
class SeedDataTest {

    @Test fun parserDeCsvTrataAspasVirgulasEAspasEscapadas() {
        assertEquals(listOf("a", "b,c", "d\"e", ""), parseCsvLine("a,\"b,c\",\"d\"\"e\","))
        assertEquals(listOf("x"), parseCsvLine("x"))
    }

    @Test fun cabecalhosSaoOsEsperadosPeloSeedLoader() {
        assertEquals(listOf("id", "name", "type"), SeedFiles.headerOf("territories.csv"))
        assertEquals(listOf("type", "unit", "warning", "critical", "source_note"), SeedFiles.headerOf("indicators.csv"))
        assertEquals(listOf("id", "territory_id", "indicator", "timestamp", "value", "origin"), SeedFiles.headerOf("readings.csv"))
        assertTrue(SeedFiles.version().isNotBlank())
    }

    @Test fun territoriosEIndicadoresSaoConsistentes() {
        val territories = SeedFiles.territories()
        assertTrue(territories.size >= 3)
        assertEquals(territories.size, territories.map { it.id }.toSet().size)
        val indicators = SeedFiles.indicators()
        assertEquals(setOf(IndicatorType.ENERGY, IndicatorType.WATER, IndicatorType.CLIMATE), indicators.map { it.type }.toSet())
        indicators.filter { it.type != IndicatorType.CLIMATE }.forEach {
            assertTrue("${it.type}: atenção < crítico", it.warningThreshold in 0.0..it.criticalThreshold && it.warningThreshold < it.criticalThreshold)
            assertTrue("${it.type}: precisa de nota de fonte", it.sourceNote.isNotBlank())
        }
    }

    @Test fun leiturasNaoTemDuplicidadeNemValoresInvalidosNemReferenciasQuebradas() {
        val territoryIds = SeedFiles.territories().map { it.id }.toSet()
        val readings = SeedFiles.readings()
        assertTrue(readings.isNotEmpty())
        assertEquals("ids duplicados", readings.size, readings.map { it.id }.toSet().size)
        assertEquals(
            "série (território, indicador, instante) duplicada",
            readings.size,
            readings.map { Triple(it.territoryId, it.indicator, it.timestamp) }.toSet().size,
        )
        readings.forEach {
            assertTrue("território inexistente: ${it.territoryId}", it.territoryId in territoryIds)
            assertTrue("valor inválido em ${it.id}", it.value.isFinite() && it.value >= 0.0)
            assertTrue("timestamp inválido em ${it.id}", it.timestamp > 0)
        }
    }

    @Test fun todoTerritorioTemEnergiaAguaEClimaComHistoricoSuficiente() {
        val readings = SeedFiles.readings()
        SeedFiles.territories().forEach { t ->
            listOf(IndicatorType.ENERGY, IndicatorType.WATER, IndicatorType.CLIMATE).forEach { type ->
                val n = readings.count { it.territoryId == t.id && it.indicator == type }
                assertTrue("${t.id}/$type com poucas leituras ($n)", n >= 14)
            }
        }
    }

    @Test fun origemDosDadosEstaSempreIdentificadaESinteticosNaoSaoMarcadosComoPublicos() {
        val readings = SeedFiles.readings()
        // água e clima nunca têm série pública diária por bairro: precisam estar marcados como sintéticos
        assertTrue(readings.filter { it.indicator != IndicatorType.ENERGY }.all { it.origin == DataSource.SINTETICA })
        val notes = SeedFiles.indicators().associate { it.type to it.sourceNote }
        val energyIsPublic = readings.any { it.indicator == IndicatorType.ENERGY && it.origin == DataSource.PUBLICA }
        if (!energyIsPublic) {
            assertTrue(notes.getValue(IndicatorType.ENERGY).startsWith("Sintética"))
        } else {
            assertTrue(notes.getValue(IndicatorType.ENERGY).startsWith("Pública"))
        }
    }

    @Test fun climaTemPrevisaoFuturaMasEnergiaEAguaNaoTemLeiturasFuturasAoFimDaSerie() {
        val readings = SeedFiles.readings()
        val lastObserved = readings.filter { it.indicator != IndicatorType.CLIMATE }.maxOf { it.timestamp }
        assertTrue(readings.any { it.indicator == IndicatorType.CLIMATE && it.timestamp > lastObserved })
    }
}
