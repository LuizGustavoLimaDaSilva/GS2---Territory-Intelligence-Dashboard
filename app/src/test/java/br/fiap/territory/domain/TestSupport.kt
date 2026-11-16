package br.fiap.territory.domain

import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.AlertLevel
import br.fiap.territory.domain.model.DataSource
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.NotificationPermission
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.AlertJustification
import br.fiap.territory.domain.notification.AlertNotifier
import br.fiap.territory.domain.repository.AlertRepository
import br.fiap.territory.domain.repository.DecisionRepository
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.model.SeedStatus
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.repository.DataSeeder
import br.fiap.territory.domain.repository.IndicatorRepository
import br.fiap.territory.domain.repository.ReadingRepository
import br.fiap.territory.domain.repository.TerritoryRepository
import br.fiap.territory.domain.repository.TimeProvider
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

const val DAY = 24L * 60 * 60 * 1000

/** "Agora" fixo dos testes: 09/11/2026 23:00 (America/Sao_Paulo). */
val NOW: Long = LocalDate.of(2026, 11, 9).atTime(LocalTime.of(23, 0))
    .atZone(br.fiap.territory.domain.format.PtBr.zone).toInstant().toEpochMilli()

/** Uma leitura por dia (às 23h), terminando em [NOW]. Funciona para energia e água. */
fun dailyReadings(
    type: IndicatorType,
    values: List<Double>,
    territoryId: String = "t1",
    origin: DataSource = DataSource.SINTETICA,
    endMillis: Long = NOW,
): List<Reading> = values.mapIndexed { i, v ->
    val ts = endMillis - (values.size - 1 - i) * DAY
    Reading("${type.name}-$territoryId-$ts", territoryId, type, ts, v, origin)
}

val energyIndicator = Indicator(IndicatorType.ENERGY, "MW", 10.0, 11.5, "Sintética (teste)")
val waterIndicator = Indicator(IndicatorType.WATER, "L/hab/dia", 168.0, 187.5, "Sintética (teste)")

fun sampleAlert(id: String = "a1", level: AlertLevel = AlertLevel.CRITICO) = Alert(
    id = id, territoryId = "t1", territoryName = "Bairro Teste", indicator = IndicatorType.ENERGY,
    level = level, title = "Alerta $id", summary = "resumo", confidence = 0.8,
    justification = AlertJustification(emptyList(), "regra", "período", "fonte"),
    forecastPeriod = "2 dias", createdAt = NOW, validUntil = NOW + 2 * DAY, origin = DataSource.SINTETICA,
)

class FakeTime(var current: Long = NOW) : TimeProvider {
    override fun now(): Long = current
}

class FakeAlertRepository : AlertRepository {
    val alerts = MutableStateFlow<List<Alert>>(emptyList())
    val notified = mutableSetOf<String>()
    override fun observeAll(): Flow<List<Alert>> = alerts
    override fun observeById(id: String): Flow<Alert?> = alerts.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun upsertAll(alerts: List<Alert>) {
        val ids = alerts.map { it.id }.toSet()
        this.alerts.value = this.alerts.value.filterNot { it.id in ids } + alerts
    }
    override suspend fun wasNotified(alertId: String) = alertId in notified
    override suspend fun markNotified(alertId: String) { notified += alertId }
}

class FakeDecisionRepository : DecisionRepository {
    val decisions = MutableStateFlow<List<Decision>>(emptyList())
    override fun observeAll(): Flow<List<Decision>> = decisions
    override fun observeForAlert(alertId: String): Flow<List<Decision>> =
        decisions.map { list -> list.filter { it.alertId == alertId }.reversed() } // mais recente primeiro, como o Room
    override suspend fun insert(decision: Decision) { decisions.value = decisions.value + decision }
    override suspend fun clearAll() { decisions.value = emptyList() }
}

class FakePreferences(notifications: Boolean = true) : PreferencesRepository {
    override val selectedTerritoryId = MutableStateFlow<String?>(null)
    override val demoMode = MutableStateFlow(false)
    override val notificationsEnabled = MutableStateFlow(notifications)
    override val lastResponsible = MutableStateFlow("")
    private var seed: String? = null
    override suspend fun setSelectedTerritory(id: String) { selectedTerritoryId.value = id }
    override suspend fun setDemoMode(enabled: Boolean) { demoMode.value = enabled }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { notificationsEnabled.value = enabled }
    override suspend fun setLastResponsible(name: String) { lastResponsible.value = name }
    override suspend fun seedVersion(): String? = seed
    override suspend fun setSeedVersion(version: String) { seed = version }
}

class FakeNotifier(private val delivers: Boolean = true) : AlertNotifier {
    val shown = mutableListOf<String>()
    val bodies = mutableListOf<String>()
    override val permission: StateFlow<NotificationPermission> = MutableStateFlow(NotificationPermission.GRANTED)
    override fun refresh() = Unit
    override fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) = Unit
    override fun notifyAlert(alertId: String, title: String, body: String): Boolean {
        if (delivers) {
            shown += alertId
            bodies += body
        }
        return delivers
    }
}

/** Território com falha opcional: cada chamada a observeAll() cria um novo fluxo (como o Room faria). */
class FakeTerritoryRepository(var territories: List<Territory>, var failing: Boolean = false) : TerritoryRepository {
    override fun observeAll(): Flow<List<Territory>> = flow {
        if (failing) throw java.io.IOException("falha simulada de leitura")
        emit(territories)
    }
}

class FakeIndicatorRepository(private val indicators: List<Indicator>) : IndicatorRepository {
    override fun observeAll(): Flow<List<Indicator>> = MutableStateFlow(indicators)
}

class FakeReadingRepository(var readings: List<Reading>, var failing: Boolean = false) : ReadingRepository {
    /** Quantas vezes o caso de uso (re)assinou as leituras: permite verificar recargas. */
    var observeCalls = 0
        private set

    override fun observe(territoryId: String, indicator: IndicatorType): Flow<List<Reading>> {
        observeCalls++
        return flow {
            if (failing) throw java.io.IOException("falha simulada de leitura")
            emit(readings.filter { it.territoryId == territoryId && it.indicator == indicator }.sortedBy { it.timestamp })
        }
    }
}

class FakeSeeder(initial: SeedStatus = SeedStatus.Done, var failNext: Boolean = false) : DataSeeder {
    private val _status = MutableStateFlow(initial)
    override val status: StateFlow<SeedStatus> = _status
    var calls = 0
    override suspend fun seedIfNeeded() {
        calls++
        _status.value = SeedStatus.Running
        _status.value = if (failNext) SeedStatus.Failed("falha simulada") else SeedStatus.Done
    }
}
