package com.stabila.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.stabila.core.domain.TremorReading

@Database(entities = [TremorReading::class], version = 3, exportSchema = false)
@TypeConverters(TremorTypeConverters::class)
abstract class TremorDatabase : RoomDatabase() {
    abstract fun tremorReadingDao(): TremorReadingDao
}
