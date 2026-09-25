package com.shiguang.app.ui.ddl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiguang.app.data.AppContainer
import com.shiguang.app.data.AppSettings
import com.shiguang.app.data.entity.DdlEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DdlUiState(
    val all: List<DdlEntity> = emptyList(),
    /** DDL 完成后自动释放天数（设置项，默认 3） */
    val releaseDays: Int = 3,
    /** 可关联的课程名（来自日程中的周期/单次日程标题） */
    val courseNames: List<String> = emptyList(),
) {
    val pending: List<DdlEntity> get() = all.filter { !it.completed }
    val completed: List<DdlEntity> get() = all.filter { it.completed }
}

class DdlViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.ddlRepository

    val uiState: StateFlow<DdlUiState> = combine(
        repo.observeAll(),
        AppSettings.ddlReleaseDays,
        container.scheduleRepository.observeAll(),
    ) { ddls, releaseDays, schedules ->
        DdlUiState(
            all = ddls,
            releaseDays = releaseDays,
            courseNames = schedules.map { it.title }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DdlUiState())

    init {
        releaseExpired()
    }

    /** 自动释放：删除“已完成且超过 N 天”的 DDL。 */
    fun releaseExpired() {
        viewModelScope.launch {
            val days = AppSettings.ddlReleaseDays.value.coerceAtLeast(1)
            val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
            repo.releaseCompletedBefore(cutoff)
        }
    }

    fun save(entity: DdlEntity) {
        viewModelScope.launch { repo.save(entity) }
    }

    fun delete(entity: DdlEntity) {
        viewModelScope.launch { repo.delete(entity) }
    }

    /** 勾选/撤销完成，完成后立即执行一次自动释放检查。 */
    fun toggleCompleted(entity: DdlEntity, completed: Boolean) {
        viewModelScope.launch {
            repo.setCompleted(entity.id, completed)
            releaseExpired()
        }
    }
}