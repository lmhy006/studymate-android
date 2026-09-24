package com.shiguang.app.ui.countdown

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.app.StudyMateApp
import com.shiguang.app.core.Countdown
import com.shiguang.app.core.CountdownDisplay
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.entity.CountdownEntity
import com.shiguang.app.ui.components.EmptyHint
import com.shiguang.app.ui.theme.TodayGreen

@Composable
fun CountdownScreen(
    viewModel: CountdownViewModel = viewModel {
        CountdownViewModel(
            (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyMateApp).container
        )
    },
) {
    val countdowns by viewModel.items.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CountdownEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    if (editing != null || showAdd) {
        CountdownEditSheet(
            editing = editing,
            onDismiss = {
                editing = null
                showAdd = false
            },
            onSave = { id, title, target, note, pin ->
                viewModel.save(id, title, target, note, pin)
                editing = null
                showAdd = false
            },
            onDelete = { entity ->
                viewModel.delete(entity)
                editing = null
            },
            onPin = { id ->
                viewModel.setPinned(id)
                editing = null
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "倒数",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = DateUtils.formatFull(DateUtils.today()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (countdowns.isEmpty()) {
                item { EmptyHint("还没有倒数日\n点击右下角 + 添加一个目标") }
            }
            items(countdowns, key = { it.id }) { item ->
                CountdownCard(entity = item, onClick = { editing = item })
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加倒数日")
        }
    }
}

@Composable
private fun CountdownCard(entity: CountdownEntity, onClick: () -> Unit) {
    val display = Countdown.display(
        target = DateUtils.fromEpochDay(entity.targetEpochDay),
        today = DateUtils.today(),
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = entity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(DateUtils.formatFull(DateUtils.fromEpochDay(entity.targetEpochDay)))
                        entity.note?.takeIf { it.isNotBlank() }?.let {
                            append(" · ").append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entity.pinned) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "桌面展示中",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = display.days.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (display) {
                        is CountdownDisplay.Upcoming -> MaterialTheme.colorScheme.primary
                        is CountdownDisplay.Today -> TodayGreen
                        is CountdownDisplay.Passed -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = Countdown.summary(display),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}