package com.shiguang.app

/**
 * 应用级配置。
 */
object AppConfig {

    /** GitHub 仓库，用于应用内检查更新（读取该仓库的 latest release 及其 APK 资产）。 */
    const val GITHUB_REPO = "lmhy006/studymate-android"

    /**
     * Gitee（码云）备用源，与 GitHub 同结构：国内网络 GitHub 不通时自动切换。
     * 需要把本仓库镜像到 Gitee 并同步发布同名 Release 与 APK 资产。
     */
    const val GITEE_REPO = "lmhy006/studymate-android"
}