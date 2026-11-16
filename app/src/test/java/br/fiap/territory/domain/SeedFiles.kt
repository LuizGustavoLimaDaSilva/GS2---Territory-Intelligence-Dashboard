package br.fiap.territory.domain

import br.fiap.territory.data.seed.parseCsvLine
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.Territory
import java.io.File

/** Lê os CSVs reais de assets/seed (o diretório de trabalho dos testes unitários é a pasta `app/`). */
object SeedFiles {
    private val dir = File("src/main/assets/seed")

    fun rows(name: String): List<Map<String, String>> {
        val lines = File(dir, name).readLines().filter { it.isNotBlank() }
        val header = parseCsvLine(lines.first())
        return lines.drop(1).map { header.zip(parseCsvLine(it)).toMap() }
    }

    fun headerOf(name: String): List<String> = parseCsvLine(File(dir, name).readLines().first())

    fun version(): String = File(dir, "seed_version.txt").readText().trim()

    fun territories() = rows("territories.csv").map { Territory(it.getValue("id"), it.getValue("name"), it.getValue("type")) }

    fun indicators() = rows("indicators.csv").map {
        Indicator(
            IndicatorType.valueOf(it.getValue("type")), it.getValue("unit"),
            it.getValue("warning").toDouble(), it.getValue("critical").toDouble(), it.getValue("source_note"),
        )
    }

    fun readings() = rows("readings.csv").map {
        Reading(
            it.getValue("id"), it.getValue("territory_id"), IndicatorType.valueOf(it.getValue("indicator")),
            it.getValue("timestamp").toLong(), it.getValue("value").toDouble(), DataSource.valueOf(it.getValue("origin")),
        )
    }
}
