package com.asa.note.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.data.entity.NoteEntity
import com.asa.note.repo.ExcerptRepository
import com.asa.note.repo.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * 一次输入，两张表一起搜。备忘录搜标题+正文，摘录搜原文+书名+我的评论。
 * 查询为空时不查库，避免一进来就把全表读出来。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    notes: NoteRepository,
    excerpts: ExcerptRepository,
) : ViewModel() {

    var query by mutableStateOf("")
        private set

    private val queryFlow = MutableStateFlow("")

    val noteHits: StateFlow<List<NoteEntity>> = queryFlow
        .flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else notes.observeMatching(text)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val excerptHits: StateFlow<List<ExcerptEntity>> = queryFlow
        .flatMapLatest { text ->
            if (text.isBlank()) flowOf(emptyList()) else excerpts.observeMatching(text)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(text: String) {
        query = text
        queryFlow.value = text
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SearchViewModel(container.notes, container.excerpts) }
        }
    }
}
