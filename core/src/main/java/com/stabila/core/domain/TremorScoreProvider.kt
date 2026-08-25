package com.stabila.core.domain

import kotlinx.coroutines.flow.Flow

interface TremorScoreProvider {
    /**
     * Gets the latest smoothed tremor score, from 0.0 (stable) to 1.0 (severe).
     */
    suspend fun getLatestScore(): Float

    /**
     * Exposes a reactive stream of the latest tremor score.
     */
    fun getScoreFlow(): Flow<Float>
}
