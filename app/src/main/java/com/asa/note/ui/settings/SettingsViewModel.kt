package com.asa.note.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.repo.ExcerptRepository
import com.asa.note.repo.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    notes: NoteRepository,
    excerpts: ExcerptRepository,
) : ViewModel() {

    val trashCount: StateFlow<Int> = combine(
        notes.observeTrash(),
        excerpts.observeTrash(),
    ) { trashedNotes, trashedExcerpts ->
        trashedNotes.size + trashedExcerpts.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container.notes, container.excerpts) }
        }
    }
}
