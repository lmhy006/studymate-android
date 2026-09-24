package com.shiguang.app.ui.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiguang.app.data.AppContainer
import com.shiguang.app.data.entity.CountdownEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CountdownViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.countdownRepository

    val items: StateFlow<List<CountdownEntity>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 新增或编辑倒数日。 */
    fun save(id: Long?, title: String, target: LocalDate, note: String, pin: Boolean) {
        viewModelScope.launch {
            if (id == null) {
                val newId = repo.save(
                    CountdownEntity(
                        title = title,
                        targetEpochDay = target.toEpochDay(),
                        note = note.ifBlank { null },
                        pinned = pin,
                    )
                )
                if (pin) repo.setPinned(newId)
            } else {
                val current = repo.getById(id) ?: return@launch
                repo.save(
                    current.copy(
                        title = title,
                        targetEpochDay = target.toEpochDay(),
                        note = note.ifBlank { null },
                    )
                )
                if (pin) repo.setPinned(id) else if (current.pinned) repo.clearPinned()
            }
            container.refreshWidget()
        }
    }

    fun delete(entity: CountdownEntity) {
        viewModelScope.launch {
            repo.delete(entity)
            container.refreshWidget()
        }
    }

    /** 置顶到桌面小组件。 */
    fun setPinned(id: Long) {
        viewModelScope.launch {
            repo.setPinned(id)
            container.refreshWidget()
        }
    }
}