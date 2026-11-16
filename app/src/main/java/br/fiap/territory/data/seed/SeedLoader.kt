package br.fiap.territory.data.seed

import android.content.Context
import androidx.room.withTransaction
import br.fiap.territory.data.local.IndicatorEntity
import br.fiap.territory.data.local.ReadingEntity
import br.fiap.territory.data.local.TerritoryDatabase
import br.fiap.territory.data.local.TerritoryEntity
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.SeedStatus
import br.fiap.territory.domain.repository.DataSeeder
import br.fiap.territory.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Carrega os CSVs de assets/seed para o Room. Só recarrega quando seed_version.txt muda
 * (ou quando o banco está vazio). Decisões e alertas já registrados nunca são apagados.
 */
@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: TerritoryDatabase,
    private val preferences: PreferencesRepository,
) : DataSeeder {

    private val _status = MutableStateFlow<SeedStatus>(SeedStatus.Running)
    override val status: StateFlow<SeedStatus> = _status.asStateFlow()

    override suspend fun seedIfNeeded() {
        _status.value = SeedStatus.Running
        try {
            val version = readText("seed/seed_version.txt").trim()
            if (preferences.seedVersion() != version || db.readings().count() == 0) {
                load()
                preferences.setSeedVersion(version)
            }
            _status.value = SeedStatus.Done
        } catch (e: Exception) {
            _status.value = SeedStatus.Failed(
                "Não foi possível carregar os dados locais (${e.message}). " +
                    "Gere os dados com tools/prepare_data.py e reinstale o app.",
            )
        }
    }

    private suspend fun load() {
        val territories = readCsv("seed/territories.csv").map {
            TerritoryEntity(it.getValue("id"), it.getValue("name"), it.getValue("type"))
        }
        val indicators = readCsv("seed/indicators.csv").map {
            IndicatorType.valueOf(it.getValue("type"))
            IndicatorEntity(
                type = it.getValue("type"),
                unit = it.getValue("unit"),
                warningThreshold = it.getValue("warning").toDouble(),
                criticalThreshold = it.getValue("critical").toDouble(),
                sourceNote = it.getValue("source_note"),
            )
        }
        val readings = readCsv("seed/readings.csv").map {
            IndicatorType.valueOf(it.getValue("indicator"))
            DataSource.valueOf(it.getValue("origin"))
            ReadingEntity(
                id = it.getValue("id"),
                territoryId = it.getValue("territory_id"),
                indicator = it.getValue("indicator"),
                timestamp = it.getValue("timestamp").toLong(),
                value = it.getValue("value").toDouble(),
                origin = it.getValue("origin"),
            )
        }
        require(territories.isNotEmpty() && indicators.isNotEmpty() && readings.isNotEmpty()) {
            "arquivos de seed vazios"
        }
        db.withTransaction {
            db.territories().clear()
            db.indicators().clear()
            db.readings().clear()
            db.territories().insertAll(territories)
            db.indicators().insertAll(indicators)
            readings.chunked(1000).forEach { db.readings().insertAll(it) }
        }
    }

    private fun readText(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun readCsv(path: String): List<Map<String, String>> {
        val lines = readText(path).lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "$path vazio" }
        val header = parseCsvLine(lines.first())
        return lines.drop(1).mapIndexed { index, line ->
            val cells = parseCsvLine(line)
            require(cells.size == header.size) { "$path linha ${index + 2}: ${cells.size} colunas, esperado ${header.size}" }
            header.zip(cells).toMap()
        }
    }
}

/** Parser mínimo de CSV com suporte a aspas ("a,b" e "" dentro do campo). */
internal fun parseCsvLine(line: String): List<String> {
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                cells += current.toString()
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    cells += current.toString()
    return cells
}
