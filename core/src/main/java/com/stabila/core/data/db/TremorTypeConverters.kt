package com.stabila.core.data.db

import androidx.room.TypeConverter
import com.stabila.core.domain.TestType

class TremorTypeConverters {
    @TypeConverter
    fun fromTestType(value: TestType): String = value.name

    @TypeConverter
    fun toTestType(value: String): TestType = TestType.valueOf(value)

    @TypeConverter
    fun fromTremorClassification(value: com.stabila.core.domain.TremorClassification?): String? = value?.name

    @TypeConverter
    fun toTremorClassification(value: String?): com.stabila.core.domain.TremorClassification? = value?.let { com.stabila.core.domain.TremorClassification.valueOf(it) }
}
