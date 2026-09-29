package com.asa.note.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.asa.note.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE isHidden = 0 ORDER BY createdAt ASC")
    fun observeVisible(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM category WHERE name = :name AND id != :exceptId")
    suspend fun countByName(name: String, exceptId: Long): Int

    /** 导出用：连隐藏的分类一起导出，否则隐藏状态在导入后会丢。 */
    @Query("SELECT * FROM category ORDER BY createdAt ASC")
    suspend fun listAll(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE category SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE category SET isHidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun delete(id: Long)
}
