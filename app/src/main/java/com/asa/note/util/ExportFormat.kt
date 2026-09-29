package com.asa.note.util

import com.asa.note.data.entity.CategoryEntity
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.data.entity.NoteEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 纯文本导出格式。**这套格式是给"以后要做的导入"留的契约 —— 动它就必须同时改导入。**
 *
 * 结构：
 * ```
 * # 备忘录导出          ← 文件头，三行 # 注释
 * # 格式版本: 1
 * # 导出时间: 2026-09-29T21:30:00+08:00
 *
 * === 分类              ← 节标记，类型可选：分类 / 备忘录 / 摘录
 * 名称: 工作
 * 隐藏: 否
 *
 * === 备忘录
 * 标题: 银行卡号
 * 分类: 工作
 * 置顶: 是
 * 创建: 2026-09-29T20:39:00+08:00
 * 修改: 2026-09-29T20:41:00+08:00
 *
 * 正文原样写在这里
 *
 * === 摘录
 * 出处: 示例书名
 * 创建: 2026-09-29T20:51:00+08:00
 *
 * 原文
 *
 * --- 评论              ← 评论是摘录内部的子节
 * 创建: 2026-09-29T20:52:00+08:00
 *
 * 我的想法
 * ```
 *
 * 解析规则（导入时按此实现）：
 * 1. 以 `=== ` 开头的整行 = 新的一节；`--- ` 开头 = 当前节里的子节。
 * 2. 节内从第一个空行前是 `键: 值` 头，空行之后到下一个标记之间是正文。
 * 3. 头的值里，换行写成 `\n`、反斜杠写成 `\\`；导入时反过来还原。
 * 4. 正文里若出现以 `=== `、`--- ` 或 `\` 开头的整行，导出时前面补一个 `\`；导入时去掉一个。
 * 5. **未知的节类型与未知的键一律忽略** —— 以后加字段时，旧版本的导入不该因此失败。
 * 6. 时间一律是带时区偏移的 ISO-8601，避免换时区后时间漂移。
 */
object ExportFormat {

    const val VERSION = 1

    private const val ITEM_PREFIX = "=== "
    private const val COMMENT_PREFIX = "--- "
    private val iso = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun fileHeader(now: Long): String = buildString {
        append("# 备忘录导出\n")
        append("# 格式版本: $VERSION\n")
        append("# 导出时间: ${isoTime(now)}\n")
    }

    /** 单条导出：同一个文件头 + 一个节，所以单条文件也能被同一个导入读进去。 */
    fun single(block: String, now: Long): String = fileHeader(now) + "\n" + block

    fun document(
        categories: List<CategoryEntity>,
        notes: List<NoteEntity>,
        excerpts: List<ExcerptEntity>,
        commentsByExcerpt: Map<Long, List<ExcerptCommentEntity>>,
        now: Long,
    ): String {
        val categoryNames = categories.associate { it.id to it.name }
        val blocks = buildList {
            categories.forEach { add(categoryBlock(it)) }
            notes.forEach { add(noteBlock(it, categoryNames[it.categoryId])) }
            excerpts.forEach { add(excerptBlock(it, commentsByExcerpt[it.id].orEmpty())) }
        }
        return buildString {
            append(fileHeader(now))
            blocks.forEach { block ->
                append('\n')
                append(block)
            }
        }
    }

    fun categoryBlock(category: CategoryEntity): String = buildString {
        append("${ITEM_PREFIX}分类\n")
        append("名称: ${headerValue(category.name)}\n")
        append("隐藏: ${yesNo(category.isHidden)}\n")
        append('\n')
    }

    fun noteBlock(note: NoteEntity, categoryName: String?): String = buildString {
        append("${ITEM_PREFIX}备忘录\n")
        append("标题: ${headerValue(note.title)}\n")
        if (!categoryName.isNullOrBlank()) {
            append("分类: ${headerValue(categoryName)}\n")
        }
        append("置顶: ${yesNo(note.isPinned)}\n")
        append("创建: ${isoTime(note.createdAt)}\n")
        append("修改: ${isoTime(note.updatedAt)}\n")
        append('\n')
        append(escapeBody(note.content))
        append('\n')
    }

    fun excerptBlock(
        excerpt: ExcerptEntity,
        comments: List<ExcerptCommentEntity>,
    ): String = buildString {
        append("${ITEM_PREFIX}摘录\n")
        if (excerpt.source.isNotBlank()) {
            append("出处: ${headerValue(excerpt.source)}\n")
        }
        append("创建: ${isoTime(excerpt.createdAt)}\n")
        append('\n')
        append(escapeBody(excerpt.text))
        append('\n')
        comments.forEach { comment ->
            append('\n')
            append("${COMMENT_PREFIX}评论\n")
            append("创建: ${isoTime(comment.createdAt)}\n")
            append('\n')
            append(escapeBody(comment.text))
            append('\n')
        }
    }

    private fun yesNo(value: Boolean): String = if (value) "是" else "否"

    private fun isoTime(millis: Long): String =
        iso.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    /** 头的值必须压成一行，否则会把节拆坏。 */
    private fun headerValue(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    private fun escapeBody(body: String): String = body
        .split("\n")
        .joinToString("\n") { line ->
            if (
                line.startsWith(ITEM_PREFIX) ||
                line.startsWith(COMMENT_PREFIX) ||
                line.startsWith("\\")
            ) {
                "\\$line"
            } else {
                line
            }
        }
}
