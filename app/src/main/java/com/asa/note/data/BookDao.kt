package com.asa.note.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.asa.note.data.entity.BookEntity
import com.asa.note.data.entity.BookGroupEntity
import kotlinx.coroutines.flow.Flow

/** 书 + 它的摘录条数 + 它归的大类。书名从摘录里派生，所以不会有孤儿书。 */
data class BookRow(val name: String, val excerptCount: Int, val groupId: Long?)

/** 大类 + 它下面有几本书、几条摘录。 */
data class BookGroupRow(val id: Long, val name: String, val bookCount: Int, val excerptCount: Int)

@Dao
interface BookDao {

    @Query("SELECT * FROM book_group ORDER BY createdAt ASC")
    fun observeGroups(): Flow<List<BookGroupEntity>>

    @Query(
        "SELECT g.id AS id, g.name AS name, " +
            "COUNT(DISTINCT e.source) AS bookCount, COUNT(e.id) AS excerptCount " +
            "FROM book_group g " +
            "LEFT JOIN book b ON b.groupId = g.id " +
            "LEFT JOIN excerpt e ON e.source = b.name AND e.deletedAt IS NULL " +
            "GROUP BY g.id ORDER BY g.createdAt ASC",
    )
    fun observeGroupRows(): Flow<List<BookGroupRow>>

    /** 书是从摘录里自动出现的（DISTINCT source），左连 book 取它的大类。 */
    @Query(
        "SELECT e.source AS name, COUNT(e.id) AS excerptCount, b.groupId AS groupId " +
            "FROM excerpt e LEFT JOIN book b ON b.name = e.source " +
            "WHERE e.deletedAt IS NULL AND e.source != '' " +
            "GROUP BY e.source ORDER BY e.source ASC",
    )
    fun observeBooks(): Flow<List<BookRow>>

    @Query("SELECT COUNT(*) FROM book_group WHERE name = :name AND id != :exceptId")
    suspend fun countGroupByName(name: String, exceptId: Long): Int

    @Insert
    suspend fun insertGroup(group: BookGroupEntity): Long

    @Query("UPDATE book_group SET name = :name WHERE id = :id")
    suspend fun renameGroup(id: Long, name: String)

    @Query("DELETE FROM book_group WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    // 书名是主键，改归属就是拿同一本书再写一次，必须 REPLACE；
    // 默认的 ABORT 会在第二次赋值时撞主键，直接抛 SQLiteConstraintException 把应用干掉。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBook(book: BookEntity)

    @Transaction
    suspend fun setBookGroup(name: String, groupId: Long?) {
        upsertBook(BookEntity(name = name, groupId = groupId, createdAt = System.currentTimeMillis()))
    }
}
