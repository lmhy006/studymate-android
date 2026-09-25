package com.shiguang.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DDL（课程作业截止）：作业内容 + 截止时间（日期+时刻，epochMilli）+ 提交方式（可选）。
 * 完成后经 N 天（配置项，默认 3）自动释放删除；过期未完成仅 App 内红色提示。
 */
@Entity(tableName = "ddls")
data class DdlEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 作业内容 */
    val title: String,
    /** 关联课程名（可空；来自日程中的课程标题） */
    val courseName: String? = null,
    /** 提交截止时间（本地时区 epochMilli） */
    val dueAt: Long,
    /** 提交方式（可选） */
    val submitMethod: String? = null,
    /** 备注（可选） */
    val note: String? = null,
    val completed: Boolean = false,
    /** 完成时间（epochMilli），用于自动释放 */
    val completedAt: Long? = null,
)