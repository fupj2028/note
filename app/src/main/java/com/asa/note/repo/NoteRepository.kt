package com.asa.note.repo

import com.asa.note.data.CategoryCount
import com.asa.note.data.NoteDao
import com.asa.note.data.entity.NoteEntity
import com.asa.note.util.LikeText
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class NoteRepository(private val dao: NoteDao) {

    fun observeActive(): Flow<List<NoteEntity>> = dao.observeActive()

    fun observeActiveIn(categoryId: Long): Flow<List<NoteEntity>> = dao.observeActiveIn(categoryId)

    fun observeTrash(): Flow<List<NoteEntity>> = dao.observeTrash()

    fun observeCountsByCategory(): Flow<List<CategoryCount>> = dao.observeCountsByCategory()

    fun observeMatching(rawQuery: String): Flow<List<NoteEntity>> =
        dao.observeMatching(LikeText.pattern(rawQuery))

    suspend fun findById(id: Long): NoteEntity? = dao.findById(id)

    /** 导出用的一次性读取。 */
    suspend fun listActive(): List<NoteEntity> = dao.listActive()

    /**
     * 编辑器专用：只带标题和正文进来，其它字段从库里读回来再合并。
     * 直接拿编辑器里的对象去 update，会把分类和置顶状态一起抹掉。
     */
    suspend fun saveDraft(id: Long, title: String, content: String): Long {
        val now = System.currentTimeMillis()
        val nextTitle = title.ifBlank { deriveTitle(content) }
        if (id == 0L) {
            return dao.insert(
                NoteEntity(title = nextTitle, content = content, createdAt = now, updatedAt = now),
            )
        }
        val existing = dao.findById(id) ?: return id
        dao.update(existing.copy(title = nextTitle, content = content, updatedAt = now))
        return id
    }

    suspend fun setPinned(id: Long, pinned: Boolean) {
        dao.findById(id)?.let { dao.update(it.copy(isPinned = pinned)) }
    }

    suspend fun setCategory(id: Long, categoryId: Long?) {
        dao.findById(id)?.let { dao.update(it.copy(categoryId = categoryId)) }
    }

    suspend fun moveToTrash(id: Long) = dao.softDelete(id, System.currentTimeMillis())

    suspend fun restore(id: Long) = dao.restore(id)

    suspend fun purge(id: Long) = dao.purge(id)

    suspend fun emptyTrash() = dao.purgeAllTrash()

    suspend fun purgeExpired() =
        dao.purgeDeletedBefore(System.currentTimeMillis() - TRASH_RETENTION_MILLIS)

    private fun deriveTitle(content: String): String =
        content.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(TITLE_FALLBACK_LENGTH)
            .orEmpty()

    companion object {
        const val TITLE_FALLBACK_LENGTH = 30
        val TRASH_RETENTION_MILLIS: Long = TimeUnit.DAYS.toMillis(30)
    }
}
