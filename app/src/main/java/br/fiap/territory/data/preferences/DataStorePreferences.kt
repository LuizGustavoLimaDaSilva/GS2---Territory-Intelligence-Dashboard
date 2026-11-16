package br.fiap.territory.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.fiap.territory.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.territoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "territory_preferences")

class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {

    private object Keys {
        val selectedTerritory = stringPreferencesKey("selected_territory")
        val demoMode = booleanPreferencesKey("demo_mode")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val responsible = stringPreferencesKey("last_responsible")
        val seedVersion = stringPreferencesKey("seed_version")
    }

    private val store get() = context.territoryDataStore

    override val selectedTerritoryId: Flow<String?> = store.data.map { it[Keys.selectedTerritory] }
    override val demoMode: Flow<Boolean> = store.data.map { it[Keys.demoMode] ?: false }
    override val notificationsEnabled: Flow<Boolean> = store.data.map { it[Keys.notifications] ?: true }
    override val lastResponsible: Flow<String> = store.data.map { it[Keys.responsible].orEmpty() }

    override suspend fun setSelectedTerritory(id: String) {
        store.edit { it[Keys.selectedTerritory] = id }
    }

    override suspend fun setDemoMode(enabled: Boolean) {
        store.edit { it[Keys.demoMode] = enabled }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { it[Keys.notifications] = enabled }
    }

    override suspend fun setLastResponsible(name: String) {
        store.edit { it[Keys.responsible] = name }
    }

    override suspend fun seedVersion(): String? = store.data.first()[Keys.seedVersion]

    override suspend fun setSeedVersion(version: String) {
        store.edit { it[Keys.seedVersion] = version }
    }
}
