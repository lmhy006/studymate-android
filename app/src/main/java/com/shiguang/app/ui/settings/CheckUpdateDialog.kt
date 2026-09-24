package com.shiguang.app.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiguang.app.BuildConfig
import com.shiguang.app.AppConfig
import com.shiguang.app.update.AppVersion
import com.shiguang.app.update.ReleaseInfo
import com.shiguang.app.update.UpdateClient
import kotlinx.coroutines.launch
import java.io.File

private sealed interface UpdateUiState {
    data object Checking : UpdateUiState
    data object Latest : UpdateUiState
    data class Found(val info: ReleaseInfo) : UpdateUiState
    data object Downloading : UpdateUiState
    data class Ready(val info: ReleaseInfo, val apk: File) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

/**
 * 检查更新对话框：
 * 检查中 -> 已是最新 / 发现新版本 -> 下载中 -> 可安装；失败展示原因并可重试。
 */
@Composable
fun CheckUpdateDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Checking) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        state = UpdateUiState.Checking
        val local = BuildConfig.VERSION_NAME
        val release = UpdateClient.fetchLatest(AppConfig.GITHUB_REPO) ?: run {
            state = UpdateUiState.Failed("网络不可用或未发布版本，请稍后重试")
            return@LaunchedEffect
        }
        if (!AppVersion.hasUpdate(local, release.versionName)) {
            state = UpdateUiState.Latest
        } else if (release.apkUrl == null) {
            state = UpdateUiState.Failed("新版本缺少 APK 资产，无法下载")
        } else {
            state = UpdateUiState.Found(release)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state is UpdateUiState.Ready) "安装更新" else "检查更新") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                when (val s = state) {
                    is UpdateUiState.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Text(
                            text = "正在检查更新…",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is UpdateUiState.Latest -> {
                        Text(
                            text = "当前已是最新版本 v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    is UpdateUiState.Found -> {
                        Text(
                            text = "发现新版本 v${s.info.versionName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        s.info.notes?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Text(
                            text = "正在下载更新…",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is UpdateUiState.Ready -> {
                        Text(
                            text = "下载完成（v${s.info.versionName}），是否立即安装？",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is UpdateUiState.Failed -> {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (val s = state) {
                is UpdateUiState.Found -> {
                    TextButton(onClick = {
                        scope.launch {
                            state = UpdateUiState.Downloading
                            val apk = UpdateClient.downloadApk(context, s.info.apkUrl!!)
                            state = if (apk != null) UpdateUiState.Ready(s.info, apk) else {
                                UpdateUiState.Failed("下载失败，请检查网络后重试")
                            }
                        }
                    }) {
                        Text("下载并安装")
                    }
                }
                is UpdateUiState.Ready -> {
                    TextButton(onClick = { UpdateClient.install(context, s.apk) }) {
                        Text("安装")
                    }
                }
                is UpdateUiState.Failed -> {
                    TextButton(onClick = { attempt++ }) { Text("重试") }
                }
                else -> Unit
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}