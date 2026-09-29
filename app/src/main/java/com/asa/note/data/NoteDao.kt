package com.asa.note.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.asa.note.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

data class CategoryCount(val categoryId: Long, val count: Int)

@Dao
interface NoteDao {

    @Query("SELECT * FROM note WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query(
        "SELECT * FROM note WHERE deletedAt IS NULL AND categoryId = :categoryId " +
            "ORDER BY isPinned DESC, updatedAt DESC",
    )
    fun observeActiveIn(categoryId: Long): Flow<List<NoteEntity>>

    @Query(
        "SELECT * FROM note WHERE deletedAt IS NULL " +
            "AND (title LIKE :pattern ESCAPE '\\' OR content LIKE :pattern ESCAPE '\\') " +
            "ORDER BY updatedAt DESC",
    )
    fun observeMatching(pattern: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun findById(id: Long): NoteEntity?

    /** 导出用：一次性把未删的笔记取出来（Flow 那套是给界面订阅的）。 */
    @Query("SELECT * FROM note WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun listActive(): List<NoteEntity>

    @Query(
        "SELECT categoryId, COUNT(*) AS count FROM note " +
            "WHERE deletedAt IS NULL AND categoryId IS NOT NULL GROUP BY categoryId",
    )
    fun observeCountsByCategory(): Flow<List<CategoryCount>>

    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Query("UPDATE note SET deletedAt = :at WHERE id = :id")
    suspend fun softDelete(id: Long, at: Long)

    @Query("UPDATE note SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun purge(id: Long)

    @Query("DELETE FROM note WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeDeletedBefore(cutoff: Long)

    @Query("DELETE FROM note WHERE deletedAt IS NOT NULL")
    suspend fun purgeAllTrash()
}
