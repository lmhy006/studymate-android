package com.shiguang.app.update

import com.shiguang.app.update.FetchResult.NoRelease
import com.shiguang.app.update.FetchResult.Release
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 双更新源（GitHub + Gitee）合并策略测试。
 */
class UpdateSourceTest {

    private fun release(tag: String) = Release(ReleaseInfo(tag, tag.trimStart('v'), "https://example.com/a.apk", null))

    @Test
    fun `GitHub 有发布则优先生效`() {
        val gh = release("v1.0.1")
        val gitee = release("v9.9.9")
        val best = UpdateClient.pickBest(gh, gitee)
        assertTrue(best is Release)
        val info = (best as Release).info
        assertEquals("v1.0.1", info.tag)
        assertEquals("1.0.1", info.versionName)
    }

    @Test
    fun `GitHub 网络失败时切到 Gitee`() {
        val best = UpdateClient.pickBest(FetchResult.Error, release("v1.0.1"))
        assertTrue(best is Release)
        val info = (best as Release).info
        assertEquals("v1.0.1", info.tag)
        assertEquals("1.0.1", info.versionName)
    }

    @Test
    fun `GitHub 无发布而 Gitee 有发布 采纳 Gitee`() {
        val best = UpdateClient.pickBest(NoRelease, release("v1.0.1"))
        assertTrue(best is Release)
    }

    @Test
    fun `任一源显示无发布则整体按无发布处理`() {
        assertEquals(NoRelease, UpdateClient.pickBest(FetchResult.Error, NoRelease))
        assertEquals(NoRelease, UpdateClient.pickBest(NoRelease, FetchResult.Error))
        assertEquals(NoRelease, UpdateClient.pickBest(NoRelease, NoRelease))
    }

    @Test
    fun `双源都失败为错误`() {
        assertEquals(FetchResult.Error, UpdateClient.pickBest(FetchResult.Error, FetchResult.Error))
    }

    @Test
    fun `Gitee JSON 结构同 GitHub 可解析`() {
        val json = """
            {
              "tag_name": "v1.0.2",
              "name": "v1.0.2",
              "body": "来自 Gitee 的更新",
              "assets": [
                {"name": "studymate-v1.0.2.apk", "browser_download_url": "https://gitee.com/lmhy006/studymate-android/releases/download/v1.0.2/studymate-v1.0.2.apk"}
              ]
            }
        """.trimIndent()
        val info = UpdateClient.parseLatestJson(json)
        assertEquals("1.0.2", info.versionName)
        assertEquals("来自 Gitee 的更新", info.notes)
        assertEquals(
            "https://gitee.com/lmhy006/studymate-android/releases/download/v1.0.2/studymate-v1.0.2.apk",
            info.apkUrl,
        )
    }
}