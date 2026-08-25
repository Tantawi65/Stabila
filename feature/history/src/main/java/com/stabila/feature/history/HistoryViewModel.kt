package com.stabila.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stabila.core.data.db.TremorReadingDao
import com.stabila.core.domain.TestType
import com.stabila.core.domain.TremorReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val tremorReadingDao: TremorReadingDao
) : ViewModel() {

    // Null means "All"
    private val _filterType = MutableStateFlow<TestType?>(null)
    val filterType: StateFlow<TestType?> = _filterType

    // Combine all readings from DB with the current filter
    val readings: StateFlow<List<TremorReading>> = combine(
        tremorReadingDao.getAllReadings(),
        _filterType
    ) { allReadings, filter ->
        if (filter == null) {
            allReadings
        } else {
            allReadings.filter { it.testType == filter }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(type: TestType?) {
        _filterType.value = type
    }
}
