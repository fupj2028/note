package com.asa.note.repo

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.asa.note.util.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface ExportResult {
    /** 成功，[location] 是给人看的相对路径，例如 Download/备忘录/备忘录导出-20260929-2130.txt */
    data class Ok(val location: String) : ExportResult

    data object Failed : ExportResult

    /** 目标已经不在了（比如导出前刚被删掉）。 */
    data object Empty : ExportResult
}

/**
 * 导出到 `Download/备忘录/`。
 *
 * 走 MediaStore 而不是 SAF 文件选择器：位置固定，用户不用每次挑目录，也照样不需要任何权限
 * （API 29+ 写自己的 Downloads 条目免权限）。代价是丢了"自选保存位置"这个能力。
 *
 * 文件名撞车时系统会自动加 " (1)"，所以返回真实的 DISPLAY_NAME 而不是我们请求的那个。
 */
class ExportRepository(
    private val context: Context,
    private val notes: NoteRepository,
    private val categories: CategoryRepository,
    private val excerpts: ExcerptRepository,
) {

    suspend fun exportAll(): ExportResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val categoryList = categories.listAll()
        val noteList = notes.listActive()
        val excerptList = excerpts.listActive()
        val commentsByExcerpt = excerpts.listCommentsOfActive().groupBy { it.excerptId }

        if (categoryList.isEmpty() && noteList.isEmpty() && excerptList.isEmpty()) {
            return@withContext ExportResult.Empty
        }

        val text = ExportFormat.document(
            categories = categoryList,
            notes = noteList,
            excerpts = excerptList,
            commentsByExcerpt = commentsByExcerpt,
            now = now,
        )
        write("备忘录导出-${shortStamp(now)}.txt", text)
    }

    suspend fun exportNote(noteId: Long): ExportResult = withContext(Dispatchers.IO) {
        val note = notes.findById(noteId) ?: return@withContext ExportResult.Empty
        val categoryName = categories.listAll().firstOrNull { it.id == note.categoryId }?.name
        val now = System.currentTimeMillis()
        val block = ExportFormat.noteBlock(note, categoryName)
        write("${safeName(note.title)}-${shortStamp(now)}.txt", ExportFormat.single(block, now))
    }

    suspend fun exportExcerpt(excerptId: Long): ExportResult = withContext(Dispatchers.IO) {
        val excerpt = excerpts.findById(excerptId) ?: return@withContext ExportResult.Empty
        val now = System.currentTimeMillis()
        val block = ExportFormat.excerptBlock(excerpt, excerpts.listComments(excerptId))
        val label = excerpt.source.ifBlank { excerpt.text.take(12) }
        write("${safeName(label)}-${shortStamp(now)}.txt", ExportFormat.single(block, now))
    }

    private fun write(fileName: String, text: String): ExportResult {
        val resolver = context.contentResolver
        val pending = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pending)
            ?: return ExportResult.Failed

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
            } ?: run {
                resolver.delete(uri, null, null)
                return ExportResult.Failed
            }
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            return ExportResult.Failed
        }

        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null,
        )

        val realName = resolver
            .query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else fileName }
            ?: fileName

        return ExportResult.Ok("${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/$realName")
    }

    private fun shortStamp(millis: Long): String =
        STAMP.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    /** 文件名里不能出现路径分隔符和控制字符，标题是用户自己写的，必须过一遍。 */
    private fun safeName(raw: String): String {
        val cleaned = raw
            .replace(Regex("[/\\\\:*?\"<>|\\r\\n\\t]"), " ")
            .trim()
            .trim('.')
            .take(40)
        return cleaned.ifBlank { "无标题" }
    }

    private companion object {
        const val FOLDER = "备忘录"
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}
