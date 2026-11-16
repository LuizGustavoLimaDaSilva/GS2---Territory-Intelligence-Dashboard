package br.fiap.territory.domain.repository

import br.fiap.territory.domain.model.Alert
import br.fiap.territory.domain.model.Decision
import br.fiap.territory.domain.model.Indicator
import br.fiap.territory.domain.model.IndicatorType
import br.fiap.territory.domain.model.Reading
import br.fiap.territory.domain.model.SeedStatus
import br.fiap.territory.domain.model.Territory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Contratos do domínio. As implementações (Room, DataStore, assets) ficam na camada data. */
interface TerritoryRepository {
    fun observeAll(): Flow<List<Territory>>
}

interface IndicatorRepository {
    fun observeAll(): Flow<List<Indicator>>
}

interface ReadingRepository {
    /** Leituras mais recentes (em ordem crescente de data) de um indicador em um território. */
    fun observe(territoryId: String, indicator: IndicatorType): Flow<List<Reading>>
}

interface AlertRepository {
    fun observeAll(): Flow<List<Alert>>
    fun observeById(id: String): Flow<Alert?>
    suspend fun upsertAll(alerts: List<Alert>)
    suspend fun wasNotified(alertId: String): Boolean
    suspend fun markNotified(alertId: String)
}

interface DecisionRepository {
    fun observeAll(): Flow<List<Decision>>
    fun observeForAlert(alertId: String): Flow<List<Decision>>
    suspend fun insert(decision: Decision)
    suspend fun clearAll()
}

interface PreferencesRepository {
    val selectedTerritoryId: Flow<String?>
    val demoMode: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val lastResponsible: Flow<String>
    suspend fun setSelectedTerritory(id: String)
    suspend fun setDemoMode(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setLastResponsible(name: String)
    suspend fun seedVersion(): String?
    suspend fun setSeedVersion(version: String)
}

interface DataSeeder {
    val status: StateFlow<SeedStatus>
    suspend fun seedIfNeeded()
}

interface TimeProvider {
    fun now(): Long
}
