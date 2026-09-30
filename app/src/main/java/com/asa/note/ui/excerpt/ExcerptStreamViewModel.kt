package com.asa.note.ui.excerpt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.BookRow
import com.asa.note.data.entity.BookGroupEntity
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.repo.BookRepository
import com.asa.note.repo.ExcerptRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 摘录的两级筛选：大类 + 书。两个都可以为空（空 = 不限）。 */
data class BookFilter(val groupId: Long? = null, val source: String? = null) {
    val isActive: Boolean get() = groupId != null || source != null
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExcerptStreamViewModel(
    private val excerpts: ExcerptRepository,
    books: BookRepository,
) : ViewModel() {

    var filter by mutableStateOf(BookFilter())
        private set

    var picking by mutableStateOf(false)
        private set

    val groups: StateFlow<List<BookGroupEntity>> = books.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val books: StateFlow<List<BookRow>> = books.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val commentsByExcerpt: StateFlow<Map<Long, List<ExcerptCommentEntity>>> =
        excerpts.observeCommentsOfActive()
            .map { all -> all.groupBy { it.excerptId } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyMap<Long, List<ExcerptCommentEntity>>(),
            )

    private val filterFlow = MutableStateFlow(BookFilter())

    val stream: StateFlow<List<ExcerptEntity>> = filterFlow
        .flatMapLatest { current ->
            when {
                // 书比大类更具体，有书就按书筛。
                current.source != null -> excerpts.observeActiveInSource(current.source)
                current.groupId != null -> excerpts.observeActiveInGroup(current.groupId)
                else -> excerpts.observeActive()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openPicker() {
        picking = true
    }

    fun dismissPicker() {
        picking = false
    }

    fun applyFilter(groupId: Long?, source: String?) {
        val next = BookFilter(groupId = groupId, source = source)
        filter = next
        filterFlow.value = next
    }

    fun clearFilter() {
        applyFilter(null, null)
    }

    fun groupNameOf(id: Long?): String? =
        groups.value.firstOrNull { it.id == id }?.name

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ExcerptStreamViewModel(container.excerpts, container.books) }
        }
    }
}
