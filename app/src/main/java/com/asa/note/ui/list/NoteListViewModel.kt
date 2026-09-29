package com.asa.note.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.entity.CategoryEntity
import com.asa.note.data.entity.NoteEntity
import com.asa.note.repo.CategoryRepository
import com.asa.note.repo.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteListViewModel(
    private val notes: NoteRepository,
    private val categories: CategoryRepository,
) : ViewModel() {

    var selectedCategoryId by mutableStateOf<Long?>(null)
        private set

    val visibleCategories: StateFlow<List<CategoryEntity>> = categories.observeVisible()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val categoryFlow = MutableStateFlow<Long?>(null)

    val visibleNotes: StateFlow<List<NoteEntity>> = categoryFlow
        .flatMapLatest { categoryId ->
            if (categoryId == null) notes.observeActive() else notes.observeActiveIn(categoryId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // 正在筛选的分类被隐藏或删掉之后，筛选栏里已经没有它了。
        // 不切回「全部」的话，界面会卡在一个看不见的分类上。
        viewModelScope.launch {
            categories.observeVisible().collect { visible ->
                val selected = categoryFlow.value
                if (selected != null && visible.none { it.id == selected }) {
                    selectCategory(null)
                }
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        selectedCategoryId = categoryId
        categoryFlow.value = categoryId
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch { notes.setPinned(note.id, !note.isPinned) }
    }

    fun moveToTrash(id: Long) {
        viewModelScope.launch { notes.moveToTrash(id) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { NoteListViewModel(container.notes, container.categories) }
        }
    }
}
