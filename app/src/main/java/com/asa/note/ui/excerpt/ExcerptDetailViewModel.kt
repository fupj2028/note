package com.asa.note.ui.excerpt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.repo.ExcerptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExcerptDetailViewModel(
    private val excerpts: ExcerptRepository,
    private val excerptId: Long,
) : ViewModel() {

    var excerpt by mutableStateOf<ExcerptEntity?>(null)
        private set
    var draft by mutableStateOf("")
        private set
    var trashed by mutableStateOf(false)
        private set

    val comments: StateFlow<List<ExcerptCommentEntity>> = excerpts.observeComments(excerptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { excerpt = excerpts.findById(excerptId) }
    }

    fun onDraftChange(value: String) {
        draft = value
    }

    fun addComment() {
        val text = draft.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            excerpts.addComment(excerptId, text)
            draft = ""
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch { excerpts.deleteComment(commentId) }
    }

    fun moveToTrash() {
        viewModelScope.launch {
            excerpts.moveToTrash(excerptId)
            trashed = true
        }
    }

    companion object {
        fun factory(container: AppContainer, excerptId: Long) = viewModelFactory {
            initializer { ExcerptDetailViewModel(container.excerpts, excerptId) }
        }
    }
}
