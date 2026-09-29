package com.asa.note.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.data.entity.NoteEntity
import com.asa.note.repo.ExcerptRepository
import com.asa.note.repo.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val notes: NoteRepository,
    private val excerpts: ExcerptRepository,
) : ViewModel() {

    val trashedNotes: StateFlow<List<NoteEntity>> = notes.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trashedExcerpts: StateFlow<List<ExcerptEntity>> = excerpts.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restoreNote(id: Long) {
        viewModelScope.launch { notes.restore(id) }
    }

    fun restoreExcerpt(id: Long) {
        viewModelScope.launch { excerpts.restore(id) }
    }

    /** 彻底删是物理删除，删了就没了。摘录的评论靠外键 CASCADE 一起走。 */
    fun purgeNote(id: Long) {
        viewModelScope.launch { notes.purge(id) }
    }

    fun purgeExcerpt(id: Long) {
        viewModelScope.launch { excerpts.purge(id) }
    }

    fun empty() {
        viewModelScope.launch {
            notes.emptyTrash()
            excerpts.emptyTrash()
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { TrashViewModel(container.notes, container.excerpts) }
        }
    }
}
