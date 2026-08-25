package com.stabila.core.di

import android.content.Context
import androidx.room.Room
import com.stabila.core.data.TremorScoreProviderImpl
import com.stabila.core.data.db.TremorDatabase
import com.stabila.core.data.db.TremorReadingDao
import com.stabila.core.domain.TremorScoreProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreDatabaseModule {

    @Provides
    @Singleton
    fun provideTremorDatabase(@ApplicationContext context: Context): TremorDatabase {
        return Room.databaseBuilder(
            context,
            TremorDatabase::class.java,
            "tremor_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideTremorReadingDao(database: TremorDatabase): TremorReadingDao {
        return database.tremorReadingDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreDomainModule {
    
    @Binds
    @Singleton
    abstract fun bindTremorScoreProvider(
        impl: TremorScoreProviderImpl
    ): TremorScoreProvider
}
