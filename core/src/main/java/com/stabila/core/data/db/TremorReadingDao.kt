package com.stabila.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.stabila.core.domain.TremorReading
import kotlinx.coroutines.flow.Flow

@Dao
interface TremorReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: TremorReading)

    @Query("SELECT * FROM tremor_readings ORDER BY timestampEpochMs DESC")
    fun getAllReadings(): Flow<List<TremorReading>>

    @Query("SELECT * FROM tremor_readings ORDER BY timestampEpochMs DESC LIMIT 10")
    fun getRecentReadings(): Flow<List<TremorReading>>

    @Query("SELECT * FROM tremor_readings WHERE timestampEpochMs >= :sinceEpochMs ORDER BY timestampEpochMs DESC")
    suspend fun getReadingsSince(sinceEpochMs: Long): List<TremorReading>
}
