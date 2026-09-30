package com.asa.note.repo

import com.asa.note.data.ExcerptDao
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.util.LikeText
import kotlinx.coroutines.flow.Flow

class ExcerptRepository(private val dao: ExcerptDao) {

    fun observeActive(): Flow<List<ExcerptEntity>> = dao.observeActive()

    fun observeActiveInSource(source: String): Flow<List<ExcerptEntity>> =
        dao.observeActiveInSource(source)

    fun observeActiveInGroup(groupId: Long): Flow<List<ExcerptEntity>> =
        dao.observeActiveInGroup(groupId)

    fun observeSources(): Flow<List<String>> = dao.observeSources()

    fun observeTrash(): Flow<List<ExcerptEntity>> = dao.observeTrash()

    fun observeComments(excerptId: Long): Flow<List<ExcerptCommentEntity>> =
        dao.observeComments(excerptId)

    fun observeCommentsOfActive(): Flow<List<ExcerptCommentEntity>> = dao.observeCommentsOfActive()

    fun observeMatching(rawQuery: String): Flow<List<ExcerptEntity>> =
        dao.observeMatching(LikeText.pattern(rawQuery))

    suspend fun findById(id: Long): ExcerptEntity? = dao.findById(id)

    /** 导出用的一次性读取。 */
    suspend fun listActive(): List<ExcerptEntity> = dao.listActive()

    suspend fun listComments(excerptId: Long): List<ExcerptCommentEntity> =
        dao.listComments(excerptId)

    suspend fun listCommentsOfActive(): List<ExcerptCommentEntity> = dao.listCommentsOfActive()

    /** 只追加：收录时刻就是此刻，不提供传入自定义时间的入口。 */
    suspend fun append(text: String, source: String): Long = dao.insert(
        ExcerptEntity(
            text = text.trim(),
            source = source.trim(),
            createdAt = System.currentTimeMillis(),
        ),
    )

    suspend fun addComment(excerptId: Long, text: String) {
        dao.insertComment(
            ExcerptCommentEntity(
                excerptId = excerptId,
                text = text.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteComment(commentId: Long) = dao.deleteComment(commentId)

    suspend fun moveToTrash(id: Long) = dao.softDelete(id, System.currentTimeMillis())

    suspend fun restore(id: Long) = dao.restore(id)

    suspend fun purge(id: Long) = dao.purge(id)

    suspend fun emptyTrash() = dao.purgeAllTrash()

    suspend fun purgeExpired() =
        dao.purgeDeletedBefore(System.currentTimeMillis() - NoteRepository.TRASH_RETENTION_MILLIS)
}
