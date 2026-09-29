package com.asa.note.repo

import com.asa.note.data.CategoryDao
import com.asa.note.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

sealed interface CategoryResult {
    data object Ok : CategoryResult
    data object BlankName : CategoryResult
    data object DuplicateName : CategoryResult
}

class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    fun observeVisible(): Flow<List<CategoryEntity>> = dao.observeVisible()

    /** 导出用的一次性读取，含隐藏的分类。 */
    suspend fun listAll(): List<CategoryEntity> = dao.listAll()

    suspend fun create(rawName: String): CategoryResult {
        val name = rawName.trim()
        if (name.isEmpty()) return CategoryResult.BlankName
        if (dao.countByName(name, exceptId = -1L) > 0) return CategoryResult.DuplicateName
        dao.insert(CategoryEntity(name = name, createdAt = System.currentTimeMillis()))
        return CategoryResult.Ok
    }

    suspend fun rename(id: Long, rawName: String): CategoryResult {
        val name = rawName.trim()
        if (name.isEmpty()) return CategoryResult.BlankName
        if (dao.countByName(name, exceptId = id) > 0) return CategoryResult.DuplicateName
        dao.rename(id, name)
        return CategoryResult.Ok
    }

    suspend fun setHidden(id: Long, hidden: Boolean) = dao.setHidden(id, hidden)

    /** 删分类不删笔记：外键是 ON DELETE SET NULL，笔记会落到「未分类」。 */
    suspend fun delete(id: Long) = dao.delete(id)
}
