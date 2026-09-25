package com.shiguang.app.ui.ddl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.DdlLogic
import com.shiguang.app.data.entity.DdlEntity
import com.shiguang.app.ui.components.EmptyHint

/**
 * DDL 列表视图（位于 日程 Tab 内）：
 * 顶部横幅提醒（今日截止/已过期），未完成在前（临近截止高亮），已完成在后（显示自动释放倒计时）。
 */
@Composable
fun DdlView(
    state: DdlUiState,
    onCardClick: (DdlEntity) -> Unit,
    onToggleCompleted: (DdlEntity, Boolean) -> Unit,
    onAddDdl: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val overdue = state.pending.filter { DdlLogic.isOverdue(it.dueAt, now) }
    val dueToday = state.pending.filter {
        !DdlLogic.isOverdue(it.dueAt, now) &&
            DateUtils.fromEpochMillis(it.dueAt).toLocalDate() == DateUtils.today()
    }

    Column(modifier) {
        if (state.pending.isNotEmpty() && (overdue.isNotEmpty() || dueToday.isNotEmpty())) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Text(
                    text = buildString {
                        if (overdue.isNotEmpty()) append("已过期 ${overdue.size} 个")
                        if (dueToday.isNotEmpty()) {
                            if (isNotEmpty()) append(" · ")
                            append("今日截止 ${dueToday.size} 个")
                        }
                        if (isNotEmpty()) append("，请尽快提交。")
                    },
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.all.isEmpty()) {
                item {
                    EmptyHint("还没有 DDL\n点右下角 + 添加作业截止（可关联课程）")
                }
            }

            if (state.pending.isNotEmpty()) {
                item {
                    Text(
                        text = "未完成（${state.pending.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.pending, key = { it.id }) { ddl ->
                    DdlCard(
                        ddl = ddl,
                        now = now,
                        releaseDays = state.releaseDays,
                        onClick = { onCardClick(ddl) },
                        onToggle = { completed -> onToggleCompleted(ddl, completed) },
                    )
                }
            }

            if (state.completed.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "已完成（${state.completed.size}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.completed, key = { it.id }) { ddl ->
                    DdlCard(
                        ddl = ddl,
                        now = now,
                        releaseDays = state.releaseDays,
                        onClick = { onCardClick(ddl) },
                        onToggle = { completed -> onToggleCompleted(ddl, completed) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DdlCard(
    ddl: DdlEntity,
    now: Long,
    releaseDays: Int,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val overdue = !ddl.completed && DdlLogic.isOverdue(ddl.dueAt, now)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ddl.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!ddl.courseName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = ddl.courseName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "截止 ${DateUtils.formatDateTime(ddl.dueAt)}" +
                        (if (overdue) "（已过期）" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                )
                ddl.submitMethod?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "提交方式：$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ddl.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (ddl.completed && ddl.completedAt != null) {
                    Spacer(Modifier.height(2.dp))
                    val days = DdlLogic.daysUntilRelease(ddl.completedAt, releaseDays, now)
                    Text(
                        text = "已完成 · ${days} 天后自动释放",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            if (ddl.completed) {
                FilledTonalButton(onClick = { onToggle(false) }) {
                    Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("已完成")
                }
            } else {
                OutlinedButton(onClick = { onToggle(true) }) {
                    Text(if (overdue) "补交完成" else "完成")
                }
            }
        }
    }
}