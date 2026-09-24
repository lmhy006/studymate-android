package com.shiguang.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** GitHub latest release 信息。 */
data class ReleaseInfo(
    val tag: String,
    /** 去掉 v 前缀的版本号，如 "1.0.1"。 */
    val versionName: String,
    /** 首选 assets 中第一个 .apk 的下载地址。 */
    val apkUrl: String?,
    val notes: String?,
)

/**
 * 应用内检查更新：读取 GitHub latest release -> 比较版本 -> 下载 APK -> 系统安装器安装。
 * 使用 HttpURLConnection（stdlib），不引入额外网络库。
 */
object UpdateClient {

    private const val API_BASE = "https://api.github.com/repos/"
    private const val USER_AGENT = "StudyMate-updater"

    /** 获取最新 release（网络请求，应在协程中调用；失败返回 null）。 */
    suspend fun fetchLatest(repo: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$API_BASE$repo/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@runCatching null
            }
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            val tag = json.getString("tag_name")
            val notes = json.optString("body").ifBlank { null }
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
            ReleaseInfo(tag = tag, versionName = tag.trim().trimStart('v'), apkUrl = apkUrl, notes = notes)
        }.getOrNull()
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