package com.shiguang.app.update

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GitHub releases/latest JSON 解析测试。
 * 字段结构（tag_name / assets[].name / browser_download_url）已用真实 Release 数据交叉验证。
 */
class UpdateClientParseTest {

    private fun releaseJson(tag: String, withApk: Boolean): String {
        val asset = if (withApk) {
            """{"name": "studymate-${tag}.apk", "browser_download_url": "https://github.com/lmhy006/studymate-android/releases/download/${tag}/studymate-${tag}.apk"}"""
        } else {
            ""
        }
        return """
            {
              "tag_name": "$tag",
              "body": "修复若干问题",
              "assets": [ $asset ]
            }
        """.trimIndent()
    }

    @Test
    fun `解析含 APK 资产的 release`() {
        val info = UpdateClient.parseLatestJson(releaseJson("v1.0.1", withApk = true))
        assertEquals("v1.0.1", info.tag)
        assertEquals("1.0.1", info.versionName) // v 前缀被剥离
        assertEquals(
            "https://github.com/lmhy006/studymate-android/releases/download/v1.0.1/studymate-v1.0.1.apk",
            info.apkUrl,
        )
        assertEquals("修复若干问题", info.notes)
    }

    @Test
    fun `无 APK 资产时 apkUrl 为 null`() {
        val info = UpdateClient.parseLatestJson(releaseJson("v1.0.1", withApk = false))
        assertEquals("1.0.1", info.versionName)
        assertNull(info.apkUrl)
    }

    @Test
    fun `缺少 assets 字段也能解析`() {
        val json = """{"tag_name":"v1.0.1","body":""}"""
        val info = UpdateClient.parseLatestJson(json)
        assertEquals("1.0.1", info.versionName)
        assertNull(info.apkUrl)
        assertNull(info.notes)
    }

    @Test
    fun `多资产时优先取第一个 apk`() {
        val json = """
            {
              "tag_name": "v2",
              "body": null,
              "assets": [
                {"name": "readme.txt", "browser_download_url": "https://example.com/readme.txt"},
                {"name": "studymate-v2.apk", "browser_download_url": "https://example.com/studymate-v2.apk"}
              ]
            }
        """.trimIndent()
        val info = UpdateClient.parseLatestJson(json)
        assertEquals("2", info.versionName)
        assertEquals("https://example.com/studymate-v2.apk", info.apkUrl)
        assertNull(info.notes) // body 为 JSON null 时按无备注处理
    }

    @Test(expected = JSONException::class)
    fun `非法 JSON 抛出异常`() {
        UpdateClient.parseLatestJson("not-json{")
    }
}