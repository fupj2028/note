package com.asa.note.ui.editor

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
import com.asa.note.repo.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteEditorViewModel(
    private val notes: NoteRepository,
    private val categories: CategoryRepository,
    noteId: Long,
) : ViewModel() {

    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set
    var saving by mutableStateOf(false)
        private set
    var savedOnce by mutableStateOf(false)
        private set
    var isPinned by mutableStateOf(false)
        private set
    var trashed by mutableStateOf(false)
        private set
    var currentCategoryId by mutableStateOf<Long?>(null)
        private set
    var pickingCategory by mutableStateOf(false)
        private set

    /** 连隐藏的分类一起列出来，否则笔记归进去之后就没法再看到了。 */
    val allCategories: StateFlow<List<CategoryEntity>> = categories.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 「⋮」里的置顶/删除/导出都是对已落库的那条笔记操作；新建还没保存时没有可操作对象。 */
    var canManage by mutableStateOf(noteId > 0L)
        private set

    /** 导出时取当前落库 id（新建笔记保存后才有值）。命令式读取，不需要触发重组。 */
    val savedId: Long
        get() = currentId

    private var currentId = noteId
    private var dirty = false
    private var saveJob: Job? = null

    init {
        if (noteId > 0L) {
            viewModelScope.launch {
                notes.findById(noteId)?.let { existing ->
                    title = existing.title
                    content = existing.content
                    isPinned = existing.isPinned
                    currentCategoryId = existing.categoryId
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        title = value
        schedule()
    }

    fun onContentChange(value: String) {
        content = value
        schedule()
    }

    /** 离开页面前调用，把还没落盘的改动立刻写掉，别被 600ms 的防抖吃掉。 */
    suspend fun flush() {
        saveJob?.cancel()
        persist()
    }

    fun togglePin() {
        if (currentId == 0L) return
        viewModelScope.launch {
            notes.setPinned(currentId, !isPinned)
            isPinned = !isPinned
        }
    }

    fun moveToTrash() {
        if (currentId == 0L) return
        viewModelScope.launch {
            saveJob?.cancel()
            notes.moveToTrash(currentId)
            trashed = true
        }
    }

    fun openCategoryPicker() {
        pickingCategory = true
    }

    fun dismissCategoryPicker() {
        pickingCategory = false
    }

    fun setCategory(categoryId: Long?) {
        if (currentId == 0L) return
        viewModelScope.launch {
            notes.setCategory(currentId, categoryId)
            currentCategoryId = categoryId
            pickingCategory = false
        }
    }

    private fun schedule() {
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private suspend fun persist() {
        if (!dirty) return
        saving = true
        currentId = notes.saveDraft(currentId, title, content)
        dirty = false
        saving = false
        savedOnce = true
        canManage = true
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 600L

        fun factory(container: AppContainer, noteId: Long) = viewModelFactory {
            initializer {
                NoteEditorViewModel(container.notes, container.categories, noteId)
            }
        }
    }
}
