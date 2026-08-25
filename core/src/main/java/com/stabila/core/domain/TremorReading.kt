package com.stabila.core.domain

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TremorClassification {
    ESSENTIAL_TREMOR,
    PARKINSONS,
    NORMAL,
    UNCLASSIFIED
}

@Entity(tableName = "tremor_readings")
data class TremorReading(
    @PrimaryKey val timestampEpochMs: Long,
    val amplitude: Float,           // peak amplitude in the 4-12Hz band
    val dominantFrequencyHz: Float, // 0 if below noise floor
    val score: Float,               // composite 0-100 score (higher = more tremor)
    val testType: TestType,
    val medicationTag: String?,     // "pre-dose" / "post-dose" / null
    val classification: TremorClassification? = null
)
