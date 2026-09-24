package com.shiguang.app.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiguang.app.data.AppContainer
import com.shiguang.app.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val all: List<ScheduleEntity> = emptyList(),
)

class ScheduleViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.scheduleRepository

    val uiState: StateFlow<ScheduleUiState> = repo.observeAll()
        .map { ScheduleUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    fun save(entity: ScheduleEntity) {
        viewModelScope.launch { repo.save(entity) }
    }

    fun delete(entity: ScheduleEntity) {
        viewModelScope.launch { repo.delete(entity) }
    }
}