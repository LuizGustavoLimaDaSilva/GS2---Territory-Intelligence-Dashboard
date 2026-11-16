package br.fiap.territory.di

import android.content.Context
import androidx.room.Room
import br.fiap.territory.data.local.AlertDao
import br.fiap.territory.data.local.DecisionDao
import br.fiap.territory.data.local.IndicatorDao
import br.fiap.territory.data.local.NotifiedAlertDao
import br.fiap.territory.data.local.ReadingDao
import br.fiap.territory.data.local.TerritoryDao
import br.fiap.territory.data.local.TerritoryDatabase
import br.fiap.territory.data.notification.AndroidAlertNotifier
import br.fiap.territory.data.preferences.DataStorePreferencesRepository
import br.fiap.territory.data.repository.RoomAlertRepository
import br.fiap.territory.data.repository.RoomDecisionRepository
import br.fiap.territory.data.repository.RoomIndicatorRepository
import br.fiap.territory.data.repository.RoomReadingRepository
import br.fiap.territory.data.repository.RoomTerritoryRepository
import br.fiap.territory.data.repository.SystemTimeProvider
import br.fiap.territory.data.seed.SeedLoader
import br.fiap.territory.domain.notification.AlertNotifier
import br.fiap.territory.domain.repository.AlertRepository
import br.fiap.territory.domain.repository.DataSeeder
import br.fiap.territory.domain.repository.DecisionRepository
import br.fiap.territory.domain.repository.IndicatorRepository
import br.fiap.territory.domain.repository.PreferencesRepository
import br.fiap.territory.domain.repository.ReadingRepository
import br.fiap.territory.domain.repository.TerritoryRepository
import br.fiap.territory.domain.repository.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun territories(impl: RoomTerritoryRepository): TerritoryRepository
    @Binds abstract fun indicators(impl: RoomIndicatorRepository): IndicatorRepository
    @Binds abstract fun readings(impl: RoomReadingRepository): ReadingRepository
    @Binds abstract fun alerts(impl: RoomAlertRepository): AlertRepository
    @Binds abstract fun decisions(impl: RoomDecisionRepository): DecisionRepository
    @Binds abstract fun preferences(impl: DataStorePreferencesRepository): PreferencesRepository
    @Binds abstract fun seeder(impl: SeedLoader): DataSeeder
    @Binds abstract fun time(impl: SystemTimeProvider): TimeProvider
}

@Module
@InstallIn(SingletonComponent::class)
object ProvidesModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): TerritoryDatabase =
        Room.databaseBuilder(context, TerritoryDatabase::class.java, "territory.db").build()

    @Provides fun territoryDao(db: TerritoryDatabase): TerritoryDao = db.territories()
    @Provides fun indicatorDao(db: TerritoryDatabase): IndicatorDao = db.indicators()
    @Provides fun readingDao(db: TerritoryDatabase): ReadingDao = db.readings()
    @Provides fun alertDao(db: TerritoryDatabase): AlertDao = db.alerts()
    @Provides fun decisionDao(db: TerritoryDatabase): DecisionDao = db.decisions()
    @Provides fun notifiedDao(db: TerritoryDatabase): NotifiedAlertDao = db.notified()

    @Provides @Singleton
    fun alertNotifier(@ApplicationContext context: Context): AlertNotifier = AndroidAlertNotifier(context)
}
