package com.stabila.core.data

import com.stabila.core.data.db.TremorReadingDao
import com.stabila.core.domain.TremorScoreProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TremorScoreProviderImpl @Inject constructor(
    private val dao: TremorReadingDao
) : TremorScoreProvider {

    override suspend fun getLatestScore(): Float {
        // Look back 24 hours (86,400,000 ms)
        val since = System.currentTimeMillis() - 86_400_000L
        val readings = dao.getReadingsSince(since)
        if (readings.isEmpty()) return 0f
        
        // Simple moving average for now
        // A more advanced formula could blend RESTING and ACTION weights (0.45 * resting + 0.55 * action)
        return readings.map { it.score }.average().toFloat()
    }

    override fun getScoreFlow(): Flow<Float> {
        return dao.getRecentReadings().map { readings ->
            if (readings.isEmpty()) 0f else readings.map { it.score }.average().toFloat()
        }
    }
}
