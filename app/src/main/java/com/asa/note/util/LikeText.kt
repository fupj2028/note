package com.asa.note.util

object LikeText {
    /** LIKE 里的 % 和 _ 是通配符，用户输进来的要转义，否则搜 "100%" 会变成匹配一切。 */
    fun escape(input: String): String =
        input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    /** 直接给出可直接拼进 SQL 的模糊匹配串。 */
    fun pattern(rawQuery: String): String = "%${escape(rawQuery.trim())}%"
}
