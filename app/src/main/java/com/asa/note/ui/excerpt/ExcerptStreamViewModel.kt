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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ExcerptStreamViewModel(private val excerpts: ExcerptRepository) : ViewModel() {

    var selectedSource by mutableStateOf<String?>(null)
        private set

    val sources: StateFlow<List<String>> = excerpts.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val commentsByExcerpt: StateFlow<Map<Long, List<ExcerptCommentEntity>>> =
        excerpts.observeCommentsOfActive()
            .map { all -> all.groupBy { it.excerptId } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyMap<Long, List<ExcerptCommentEntity>>(),
            )

    private val sourceFlow = MutableStateFlow<String?>(null)

    val stream: StateFlow<List<ExcerptEntity>> = sourceFlow
        .flatMapLatest { source ->
            if (source == null) excerpts.observeActive() else excerpts.observeActiveInSource(source)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectSource(source: String?) {
        selectedSource = source
        sourceFlow.value = source
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ExcerptStreamViewModel(container.excerpts) }
        }
    }
}
