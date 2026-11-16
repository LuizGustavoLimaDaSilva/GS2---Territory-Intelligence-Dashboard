package br.fiap.territory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.fiap.territory.data.local.DecisionEntity
import br.fiap.territory.data.local.NotifiedAlertEntity
import br.fiap.territory.data.local.ReadingEntity
import br.fiap.territory.data.local.TerritoryDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDaoTest {
    private lateinit var db: TerritoryDatabase

    @Before fun open() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TerritoryDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun close() = db.close()

    private fun reading(i: Int) = ReadingEntity("r$i", "t1", "ENERGY", 1_000L * i, i.toDouble(), "SINTETICA")

    @Test fun leiturasVoltamEmOrdemCrescenteELimitadasAsMaisRecentes() = runBlocking {
        db.readings().insertAll((1..10).map { reading(it) })
        val result = db.readings().observe("t1", "ENERGY", 3).first()
        assertEquals(listOf(8.0, 9.0, 10.0), result.map { it.value })
    }

    @Test fun leituraDuplicadaENaoSobrescreve() = runBlocking {
        db.readings().insertAll(listOf(reading(1)))
        db.readings().insertAll(listOf(reading(1).copy(value = 99.0)))
        assertEquals(1.0, db.readings().observe("t1", "ENERGY", 10).first().single().value, 0.0)
    }

    @Test fun decisoesSaoPersistidasEConsultadasPorAlerta() = runBlocking {
        db.decisions().insert(DecisionEntity("d1", "a1", "Maria", "ACATAR", "ok", 10L))
        db.decisions().insert(DecisionEntity("d2", "a2", "João", "ADIAR", "", 20L))
        assertEquals(listOf("d2", "d1"), db.decisions().observeAll().first().map { it.id })
        assertEquals(listOf("d1"), db.decisions().observeForAlert("a1").first().map { it.id })
        db.decisions().clear()
        assertEquals(0, db.decisions().observeAll().first().size)
    }

    @Test fun notificacaoRegistradaUmaVez() = runBlocking {
        db.notified().mark(NotifiedAlertEntity("a1"))
        db.notified().mark(NotifiedAlertEntity("a1"))
        assertEquals(1, db.notified().count("a1"))
    }
}
