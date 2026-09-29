package com.asa.note.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcerptDao {

    @Query("SELECT * FROM excerpt WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<ExcerptEntity>>

    @Query(
        "SELECT * FROM excerpt WHERE deletedAt IS NULL AND source = :source " +
            "ORDER BY createdAt DESC",
    )
    fun observeActiveInSource(source: String): Flow<List<ExcerptEntity>>

    @Query(
        "SELECT DISTINCT source FROM excerpt WHERE deletedAt IS NULL AND source != '' " +
            "ORDER BY source ASC",
    )
    fun observeSources(): Flow<List<String>>

    @Query("SELECT * FROM excerpt WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<ExcerptEntity>>

    @Query("SELECT * FROM excerpt WHERE id = :id")
    suspend fun findById(id: Long): ExcerptEntity?

    @Query("SELECT * FROM excerpt WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun listActive(): List<ExcerptEntity>

    @Query("SELECT * FROM excerpt_comment WHERE excerptId = :excerptId ORDER BY createdAt ASC")
    suspend fun listComments(excerptId: Long): List<ExcerptCommentEntity>

    @Query(
        "SELECT c.* FROM excerpt_comment c JOIN excerpt e ON e.id = c.excerptId " +
            "WHERE e.deletedAt IS NULL ORDER BY c.createdAt ASC",
    )
    suspend fun listCommentsOfActive(): List<ExcerptCommentEntity>

    /** 跨表搜索：原文、书名、以及我自己的评论。 */
    @Query(
        "SELECT * FROM excerpt WHERE deletedAt IS NULL AND (" +
            "text LIKE :pattern ESCAPE '\\' OR source LIKE :pattern ESCAPE '\\' " +
            "OR id IN (SELECT excerptId FROM excerpt_comment WHERE text LIKE :pattern ESCAPE '\\')" +
            ") ORDER BY createdAt DESC",
    )
    fun observeMatching(pattern: String): Flow<List<ExcerptEntity>>

    @Insert
    suspend fun insert(excerpt: ExcerptEntity): Long

    @Query("UPDATE excerpt SET deletedAt = :at WHERE id = :id")
    suspend fun softDelete(id: Long, at: Long)

    @Query("UPDATE excerpt SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM excerpt WHERE id = :id")
    suspend fun purge(id: Long)

    @Query("DELETE FROM excerpt WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Query("DELETE FROM excerpt WHERE deletedAt IS NOT NULL")
    suspend fun purgeAllTrash()

    @Query("SELECT * FROM excerpt_comment WHERE excerptId = :excerptId ORDER BY createdAt ASC")
    fun observeComments(excerptId: Long): Flow<List<ExcerptCommentEntity>>

    /** 流页面一次订阅全部评论，在内存里按摘录分组，避免逐条查。 */
    @Query(
        "SELECT c.* FROM excerpt_comment c JOIN excerpt e ON e.id = c.excerptId " +
            "WHERE e.deletedAt IS NULL ORDER BY c.createdAt ASC",
    )
    fun observeCommentsOfActive(): Flow<List<ExcerptCommentEntity>>

    @Insert
    suspend fun insertComment(comment: ExcerptCommentEntity): Long

    @Query("DELETE FROM excerpt_comment WHERE id = :id")
    suspend fun deleteComment(id: Long)
}
