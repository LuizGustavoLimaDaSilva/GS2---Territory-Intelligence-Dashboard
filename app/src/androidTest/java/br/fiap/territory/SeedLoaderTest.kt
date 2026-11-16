package br.fiap.territory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.fiap.territory.data.local.TerritoryDatabase
import br.fiap.territory.data.seed.SeedLoader
import br.fiap.territory.domain.model.SeedStatus
import br.fiap.territory.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Inicialização do banco Room + carga real dos CSVs empacotados em assets/seed. */
@RunWith(AndroidJUnit4::class)
class SeedLoaderTest {
    private lateinit var context: Context
    private lateinit var db: TerritoryDatabase
    private val prefs = InMemoryPreferences()

    @Before fun open() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TerritoryDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun close() = db.close()

    @Test fun carregaTerritoriosIndicadoresELeiturasEFicaDone() = runBlocking {
        val loader = SeedLoader(context, db, prefs)
        loader.seedIfNeeded()
        assertEquals(SeedStatus.Done, loader.status.value)
        assertEquals(3, db.territories().observeAll().first().size)
        assertEquals(3, db.indicators().observeAll().first().size)
        assertTrue(db.readings().count() > 1000)
        assertTrue(prefs.seedVersion() != null)
    }

    @Test fun segundaExecucaoComAMesmaVersaoNaoRecarrega() = runBlocking {
        val loader = SeedLoader(context, db, prefs)
        loader.seedIfNeeded()
        val first = db.readings().count()
        loader.seedIfNeeded()
        assertEquals(first, db.readings().count())
    }

    @Test fun carregamentoNaoApagaDecisoesJaRegistradas() = runBlocking {
        db.decisions().insert(br.fiap.territory.data.local.DecisionEntity("d1", "a1", "Maria", "ACATAR", "", 1L))
        SeedLoader(context, db, prefs).seedIfNeeded()
        assertEquals(1, db.decisions().observeAll().first().size)
    }
}

private class InMemoryPreferences : PreferencesRepository {
    override val selectedTerritoryId = MutableStateFlow<String?>(null)
    override val demoMode = MutableStateFlow(false)
    override val notificationsEnabled = MutableStateFlow(true)
    override val lastResponsible = MutableStateFlow("")
    private var seed: String? = null
    override suspend fun setSelectedTerritory(id: String) { selectedTerritoryId.value = id }
    override suspend fun setDemoMode(enabled: Boolean) { demoMode.value = enabled }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { notificationsEnabled.value = enabled }
    override suspend fun setLastResponsible(name: String) { lastResponsible.value = name }
    override suspend fun seedVersion(): String? = seed
    override suspend fun setSeedVersion(version: String) { seed = version }
}
