package com.shiguang.app.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.AppContainer
import com.shiguang.app.data.entity.HabitEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitUiState(
    val today: LocalDate = DateUtils.today(),
    val habits: List<HabitEntity> = emptyList(),
    /** habitId -> 已打卡的 epochDay 集合 */
    val recordsByHabit: Map<Long, Set<Long>> = emptyMap(),
)

class HabitViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.habitRepository

    val uiState: StateFlow<HabitUiState> =
        combine(repo.observeHabits(), repo.observeAllRecords()) { habits, records ->
            HabitUiState(
                today = DateUtils.today(),
                habits = habits,
                recordsByHabit = records
                    .groupBy { it.habitId }
                    .mapValues { (_, list) -> list.map { it.epochDay }.toSet() },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitUiState())

    /** 打卡/撤销打卡（今天，或日历中某周期日）。 */
    fun toggleCheckIn(habit: HabitEntity, epochDay: Long, currentlyChecked: Boolean) {
        viewModelScope.launch {
            if (currentlyChecked) repo.undoCheckIn(habit.id, epochDay)
            else repo.checkIn(habit.id, epochDay)
        }
    }

    fun addHabit(name: String, emoji: String, intervalDays: Int) {
        viewModelScope.launch {
            repo.addHabit(name, emoji, intervalDays)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repo.deleteHabit(habit)
        }
    }
}