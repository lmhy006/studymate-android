package com.shiguang.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.shiguang.app.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 最新 release 信息（GitHub 与 Gitee 结构一致）。 */
data class ReleaseInfo(
    val tag: String,
    /** 去掉 v 前缀的版本号，如 "1.0.1"。 */
    val versionName: String,
    /** 首选 assets 中第一个 .apk 的下载地址。 */
    val apkUrl: String?,
    val notes: String?,
)

/** 检查更新的结果。 */
sealed interface FetchResult {
    /** 拉取到一个 Release。 */
    data class Release(val info: ReleaseInfo) : FetchResult

    /** 仓库不存在或还没有任何 Release（视为当前无需更新）。 */
    data object NoRelease : FetchResult

    /** 网络或其它错误。 */
    data object Error : FetchResult
}

/**
 * 应用内检查更新：**Gitee（码云）为默认源**（国内直连稳定），GitHub 为备用；
 * 检查更新与下载 APK 都会在默认源失败时自动回退到备用源。
 * 使用 HttpURLConnection（stdlib），不引入额外网络库。
 */
object UpdateClient {

    private const val GITHUB_API = "https://api.github.com/repos/"
    private const val GITEE_API = "https://gitee.com/api/v5/repos/"
    private const val USER_AGENT = "StudyMate-updater"

    /** 合并双源结果：任一源返回 Release 时，优先采用 Gitee（默认源）。 */
    internal fun pickBest(github: FetchResult, gitee: FetchResult): FetchResult = when {
        gitee is FetchResult.Release -> gitee
        github is FetchResult.Release -> github
        github == FetchResult.NoRelease || gitee == FetchResult.NoRelease -> FetchResult.NoRelease
        else -> FetchResult.Error
    }

    /** Gitee（默认） -> GitHub（备用）依次尝试，返回较优结果。 */
    suspend fun fetchLatest(): FetchResult = withContext(Dispatchers.IO) {
        val gitee = fetchLatestFrom("$GITEE_API${AppConfig.GITEE_REPO}/releases/latest")
        if (gitee is FetchResult.Release) {
            gitee
        } else {
            val github = fetchLatestFrom("$GITHUB_API${AppConfig.GITHUB_REPO}/releases/latest")
            pickBest(github, gitee)
        }
    }

    /** 拉取单个源（GitHub 或 Gitee，JSON 结构一致）。 */
    internal suspend fun fetchLatestFrom(apiUrl: String): FetchResult = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json, application/json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            try {
                when (conn.responseCode) {
                    HttpURLConnection.HTTP_OK -> FetchResult.Release(parseLatest(conn))
                    HttpURLConnection.HTTP_NOT_FOUND -> FetchResult.NoRelease
                    else -> FetchResult.Error
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(FetchResult.Error)
    }

    private fun parseLatest(conn: HttpURLConnection): ReleaseInfo {
        val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return parseLatestJson(text)
    }

    /** 解析 releases/latest 的 JSON 正文（GitHub 与 Gitee 结构一致；纯函数，可单测）。 */
    internal fun parseLatestJson(text: String): ReleaseInfo {
        val json = JSONObject(text)
        val tag = json.getString("tag_name")
        val notes = json.optString("body")
            .takeIf { it.isNotBlank() && it != "null" }
        var apkUrl: String? = null
        val assets = json.optJSONArray("assets") ?: JSONArray()
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            if (name.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.getString("browser_download_url")
                break
            }
        }
        return ReleaseInfo(tag = tag, versionName = tag.trim().trimStart('v'), apkUrl = apkUrl, notes = notes)
    }

    /** 下载 APK 到 cacheDir/updates/，返回文件；失败返回 null。 */
    suspend fun downloadApk(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "studymate-update.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@runCatching null
            }
            conn.inputStream.use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
            target
        }.getOrNull()
    }

    /** 默认源（Gitee）下载失败时回退到备用源（GitHub）再试。 */
    suspend fun downloadApkWithFallback(context: Context, primaryUrl: String?): File? {
        if (primaryUrl == null) return null
        val direct = downloadApk(context, primaryUrl)
        if (direct != null) return direct
        // 默认源下载失败：从 GitHub 备用源取资产 URL 再试
        return downloadApkFromGithub(context)
    }

    private suspend fun downloadApkFromGithub(context: Context): File? {
        val latest = fetchLatestFrom("$GITHUB_API${AppConfig.GITHUB_REPO}/releases/latest")
        val altUrl = (latest as? FetchResult.Release)?.info?.apkUrl ?: return null
        return downloadApk(context, altUrl)
    }

    /** 通过系统安装器安装 APK（FileProvider 授权）。 */
    fun install(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}