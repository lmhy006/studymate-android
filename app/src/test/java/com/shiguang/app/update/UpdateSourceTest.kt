package com.shiguang.app.update

import com.shiguang.app.update.FetchResult.NoRelease
import com.shiguang.app.update.FetchResult.Release
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 双更新源（Gitee 默认优先 + GitHub 备用）合并策略测试。
 */
class UpdateSourceTest {

    private fun release(tag: String) = Release(ReleaseInfo(tag, tag.trimStart('v'), "https://example.com/a.apk", null))

    @Test
    fun `Gitee 有发布则优先生效（默认源）`() {
        val github = release("v9.9.9")
        val gitee = release("v1.0.2")
        val best = UpdateClient.pickBest(github, gitee)
        assertTrue(best is Release)
        val info = (best as Release).info
        assertEquals("v1.0.2", info.tag)
        assertEquals("1.0.2", info.versionName)
    }

    @Test
    fun `Gitee 失败时切到 GitHub 备用源`() {
        val best = UpdateClient.pickBest(release("v1.0.2"), FetchResult.Error)
        assertTrue(best is Release)
        assertEquals("v1.0.2", (best as Release).info.tag)
    }

    @Test
    fun `Gitee 无发布而 GitHub 有发布 采纳 GitHub`() {
        val best = UpdateClient.pickBest(release("v1.0.2"), NoRelease)
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
                {"name": "studymate-v1.0.2.apk", "browser_download_url": "https://gitee.com/zhindex/studymate-android/releases/download/v1.0.2/studymate-v1.0.2.apk"}
              ]
            }
        """.trimIndent()
        val info = UpdateClient.parseLatestJson(json)
        assertEquals("1.0.2", info.versionName)
        assertEquals("来自 Gitee 的更新", info.notes)
        assertEquals(
            "https://gitee.com/zhindex/studymate-android/releases/download/v1.0.2/studymate-v1.0.2.apk",
            info.apkUrl,
        )
    }
}