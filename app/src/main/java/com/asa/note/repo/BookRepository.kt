package com.asa.note.repo

import com.asa.note.data.BookDao
import com.asa.note.data.BookGroupRow
import com.asa.note.data.BookRow
import com.asa.note.data.entity.BookGroupEntity
import kotlinx.coroutines.flow.Flow

/** 摘录的两级分类：大类（book_group）+ 书（book）。跟备忘录的「分类」是两套，互不共用。 */
class BookRepository(private val dao: BookDao) {

    fun observeGroups(): Flow<List<BookGroupEntity>> = dao.observeGroups()

    fun observeGroupRows(): Flow<List<BookGroupRow>> = dao.observeGroupRows()

    fun observeBooks(): Flow<List<BookRow>> = dao.observeBooks()

    suspend fun createGroup(rawName: String): NameResult {
        val name = rawName.trim()
        if (name.isEmpty()) return NameResult.BlankName
        if (dao.countGroupByName(name, exceptId = -1L) > 0) return NameResult.DuplicateName
        dao.insertGroup(BookGroupEntity(name = name, createdAt = System.currentTimeMillis()))
        return NameResult.Ok
    }

    suspend fun renameGroup(id: Long, rawName: String): NameResult {
        val name = rawName.trim()
        if (name.isEmpty()) return NameResult.BlankName
        if (dao.countGroupByName(name, exceptId = id) > 0) return NameResult.DuplicateName
        dao.renameGroup(id, name)
        return NameResult.Ok
    }

    /** 删大类不删书、更不删摘录：外键是 ON DELETE SET NULL，书会落到「未归类」。 */
    suspend fun deleteGroup(id: Long) = dao.deleteGroup(id)

    suspend fun setBookGroup(name: String, groupId: Long?) = dao.setBookGroup(name, groupId)
}
