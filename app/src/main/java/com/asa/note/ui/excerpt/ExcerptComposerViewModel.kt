package com.asa.note.ui.excerpt

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.repo.ExcerptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExcerptComposerViewModel(
    private val excerpts: ExcerptRepository,
    initialSource: String,
) : ViewModel() {

    var text by mutableStateOf("")
        private set
    var source by mutableStateOf(initialSource)
        private set
    var comment by mutableStateOf("")
        private set
    var pickingSource by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set

    val knownSources: StateFlow<List<String>> = excerpts.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onTextChange(value: String) {
        text = value
    }

    fun onSourceChange(value: String) {
        source = value
    }

    fun onCommentChange(value: String) {
        comment = value
    }

    fun openSourcePicker() {
        pickingSource = true
    }

    fun dismissSourcePicker() {
        pickingSource = false
    }

    fun pickSource(value: String) {
        source = value
        pickingSource = false
    }

    /** 收录时刻由仓库在写入时取当前时间，这里不传时间。 */
    fun save() {
        if (text.isBlank()) return
        viewModelScope.launch {
            val newId = excerpts.append(text, source)
            val firstComment = comment.trim()
            if (firstComment.isNotEmpty()) {
                excerpts.addComment(newId, firstComment)
            }
            saved = true
        }
    }

    companion object {
        fun factory(container: AppContainer, initialSource: String) = viewModelFactory {
            initializer { ExcerptComposerViewModel(container.excerpts, initialSource) }
        }
    }
}
