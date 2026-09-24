package com.shiguang.app.update

/**
 * 版本号比较（纯函数，可单测）。
 * 支持 "1.2.3" / "v1.2" / "2" 形式的点分版本号。
 */
object AppVersion {

    /** 返回 -1（local 较旧）/ 0（相等）/ 1（local 较新）。 */
    fun compare(local: String, remote: String): Int {
        val l = local.trim().trimStart('v').split('.')
        val r = remote.trim().trimStart('v').split('.')
        val n = maxOf(l.size, r.size)
        for (i in 0 until n) {
            val a = l.getOrElse(i) { "0" }.toIntOrNull() ?: 0
            val b = r.getOrElse(i) { "0" }.toIntOrNull() ?: 0
            if (a != b) return if (a < b) -1 else 1
        }
        return 0
    }

    /** 远端是否比本地新（compare(local, remote) < 0）。 */
    fun hasUpdate(local: String, remote: String): Boolean = compare(local, remote) < 0
}