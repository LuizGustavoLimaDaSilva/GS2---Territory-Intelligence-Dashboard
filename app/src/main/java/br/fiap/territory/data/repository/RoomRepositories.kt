package br.fiap.territory.data.repository

import br.fiap.territory.data.local.AlertDao
import br.fiap.territory.data.local.DecisionDao
import br.fiap.territory.data.local.IndicatorDao
import br.fiap.territory.data.local.NotifiedAlertDao
import br.fiap.territory.data.local.NotifiedAlertEntity
import br.fiap.territory.data.local.ReadingDao
import br.fiap.territory.data.local.TerritoryDao
import br.fiap.territory.data.local.toDomain
import br.fiap.territory.data.local.toEntity
import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.Territory
import br.fiap.territory.domain.repository.AlertRepository
import br.fiap.territory.domain.repository.DecisionRepository
import br.fiap.territory.domain.repository.IndicatorRepository
import br.fiap.territory.domain.repository.ReadingRepository
import br.fiap.territory.domain.repository.TerritoryRepository
import br.fiap.territory.domain.repository.TimeProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Dias de histórico lidos do banco por indicador (o motor usa 14, o gráfico mostra 21). */
private const val HISTORY_DAYS = 45

class RoomTerritoryRepository @Inject constructor(private val dao: TerritoryDao) : TerritoryRepository {
    override fun observeAll(): Flow<List<Territory>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
}

class RoomIndicatorRepository @Inject constructor(private val dao: IndicatorDao) : IndicatorRepository {
    override fun observeAll(): Flow<List<Indicator>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
}

class RoomReadingRepository @Inject constructor(private val dao: ReadingDao) : ReadingRepository {
    override fun observe(territoryId: String, indicator: IndicatorType): Flow<List<Reading>> =
        dao.observe(territoryId, indicator.name, indicator.samplesPerDay * HISTORY_DAYS)
            .map { list -> list.map { it.toDomain() } }
}

class RoomAlertRepository @Inject constructor(
    private val alerts: AlertDao,
    private val notified: NotifiedAlertDao,
) : AlertRepository {
    override fun observeAll(): Flow<List<Alert>> = alerts.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Alert?> = alerts.observeById(id).map { it?.toDomain() }

    override suspend fun upsertAll(alerts: List<Alert>) {
        if (alerts.isNotEmpty()) this.alerts.upsertAll(alerts.map { it.toEntity() })
    }

    override suspend fun wasNotified(alertId: String): Boolean = notified.count(alertId) > 0

    override suspend fun markNotified(alertId: String) = notified.mark(NotifiedAlertEntity(alertId))
}

class RoomDecisionRepository @Inject constructor(private val dao: DecisionDao) : DecisionRepository {
    override fun observeAll(): Flow<List<Decision>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeForAlert(alertId: String): Flow<List<Decision>> =
        dao.observeForAlert(alertId).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(decision: Decision) = dao.insert(decision.toEntity())

    override suspend fun clearAll() = dao.clear()
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}
