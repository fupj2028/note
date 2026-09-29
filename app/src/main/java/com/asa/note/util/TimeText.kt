package com.asa.note.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeText {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val timeOnly = DateTimeFormatter.ofPattern("HH:mm")
    private val dayHeader = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    private val fullStamp = DateTimeFormatter.ofPattern("M月d日 HH:mm")

    private fun at(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    /** 列表里显示：今天给时分，昨天给「昨天」，更早给「9/26」。 */
    fun listTime(millis: Long): String {
        val moment = at(millis)
        val today = LocalDate.now(zone)
        return when (moment.toLocalDate()) {
            today -> moment.format(timeOnly)
            today.minusDays(1) -> "昨天"
            else -> "${moment.monthValue}/${moment.dayOfMonth}"
        }
    }

    /** 摘录流的分节标题：9月28日 星期日 */
    fun dayHeader(millis: Long): String = at(millis).format(dayHeader)

    /** 完整时刻：9月28日 14:32 */
    fun stamp(millis: Long): String = at(millis).format(fullStamp)

    fun timeOnly(millis: Long): String = at(millis).format(timeOnly)

    fun dayKey(millis: Long): LocalDate = at(millis).toLocalDate()
}
