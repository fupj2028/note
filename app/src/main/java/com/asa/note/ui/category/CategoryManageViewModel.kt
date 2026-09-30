package com.asa.note.ui.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.entity.CategoryEntity
import com.asa.note.repo.CategoryRepository
import com.asa.note.repo.NameResult
import com.asa.note.repo.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRow(val category: CategoryEntity, val noteCount: Int)

/** [id] 为空表示新建，否则是重命名。 */
data class NameDialog(val id: Long?, val text: String, val error: NameResult?)

class CategoryManageViewModel(
    private val categories: CategoryRepository,
    notes: NoteRepository,
) : ViewModel() {

    val rows: StateFlow<List<CategoryRow>> = combine(
        categories.observeAll(),
        notes.observeCountsByCategory(),
    ) { categoryList, counts ->
        val countById = counts.associate { it.categoryId to it.count }
        categoryList.map { CategoryRow(it, countById[it.id] ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var nameDialog by mutableStateOf<NameDialog?>(null)
        private set

    var deleteTarget by mutableStateOf<CategoryRow?>(null)
        private set

    fun startCreate() {
        nameDialog = NameDialog(id = null, text = "", error = null)
    }

    fun startRename(row: CategoryRow) {
        nameDialog = NameDialog(id = row.category.id, text = row.category.name, error = null)
    }

    fun onNameChange(text: String) {
        nameDialog = nameDialog?.copy(text = text, error = null)
    }

    fun dismissNameDialog() {
        nameDialog = null
    }

    /** 重名与空名由仓库判定，不在这里重复一套规则。 */
    fun submitName() {
        val dialog = nameDialog ?: return
        viewModelScope.launch {
            val result = if (dialog.id == null) {
                categories.create(dialog.text)
            } else {
                categories.rename(dialog.id, dialog.text)
            }
            nameDialog = if (result == NameResult.Ok) null else dialog.copy(error = result)
        }
    }

    fun toggleHidden(row: CategoryRow) {
        viewModelScope.launch { categories.setHidden(row.category.id, !row.category.isHidden) }
    }

    fun askDelete(row: CategoryRow) {
        deleteTarget = row
    }

    fun dismissDelete() {
        deleteTarget = null
    }

    /** 删分类不删笔记：外键是 ON DELETE SET NULL，里面的笔记会落到「未分类」。 */
    fun confirmDelete() {
        val target = deleteTarget ?: return
        viewModelScope.launch {
            categories.delete(target.category.id)
            deleteTarget = null
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { CategoryManageViewModel(container.categories, container.notes) }
        }
    }
}
