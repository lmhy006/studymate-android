package com.shiguang.app

/**
 * 应用级配置。
 */
object AppConfig {

    /** GitHub 仓库（备用源：检查更新时 Gitee 失败才尝试）。 */
    const val GITHUB_REPO = "lmhy006/studymate-android"

    /**
     * Gitee（码云）更新源（默认优先：国内直连稳定）。与 GitHub 同结构：
     * 仓库需发布同名 Release（tag 带 v 前缀）并上传 .apk 资产。
     */
    const val GITEE_REPO = "zhindex/studymate-android"
}