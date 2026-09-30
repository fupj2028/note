package com.asa.note.ui.book

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.asa.note.AppContainer
import com.asa.note.data.BookGroupRow
import com.asa.note.data.BookRow
import com.asa.note.repo.BookRepository
import com.asa.note.repo.NameResult
import com.asa.note.ui.category.NameDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookManageViewModel(private val books: BookRepository) : ViewModel() {

    val groupRows: StateFlow<List<BookGroupRow>> = books.observeGroupRows()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bookRows: StateFlow<List<BookRow>> = books.observeBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var nameDialog by mutableStateOf<NameDialog?>(null)
        private set

    var pickTarget by mutableStateOf<BookRow?>(null)
        private set

    var deleteTarget by mutableStateOf<BookGroupRow?>(null)
        private set

    /** 在「挑大类」里新建大类时记住是哪本书，建完直接把它归进去，省一次选择。 */
    private var assignAfterCreate: String? = null

    fun startCreateGroup() {
        assignAfterCreate = null
        nameDialog = NameDialog(id = null, text = "", error = null)
    }

    fun startRenameGroup(row: BookGroupRow) {
        assignAfterCreate = null
        nameDialog = NameDialog(id = row.id, text = row.name, error = null)
    }

    fun startCreateGroupForBook(book: BookRow) {
        assignAfterCreate = book.name
        pickTarget = null
        nameDialog = NameDialog(id = null, text = "", error = null)
    }

    fun onNameChange(text: String) {
        nameDialog = nameDialog?.copy(text = text, error = null)
    }

    fun dismissNameDialog() {
        nameDialog = null
        assignAfterCreate = null
    }

    fun submitName() {
        val dialog = nameDialog ?: return
        viewModelScope.launch {
            val result = if (dialog.id == null) {
                books.createGroup(dialog.text)
            } else {
                books.renameGroup(dialog.id, dialog.text)
            }
            if (result != NameResult.Ok) {
                nameDialog = dialog.copy(error = result)
                return@launch
            }
            val target = assignAfterCreate
            if (dialog.id == null && target != null) {
                val newId = books.observeGroupRows().first()
                    .firstOrNull { it.name == dialog.text.trim() }
                    ?.id
                if (newId != null) books.setBookGroup(target, newId)
            }
            nameDialog = null
            assignAfterCreate = null
        }
    }

    fun askDeleteGroup(row: BookGroupRow) {
        deleteTarget = row
    }

    fun dismissDeleteGroup() {
        deleteTarget = null
    }

    /** 删大类不删书、更不删摘录：书会落到「未归类」。 */
    fun confirmDeleteGroup() {
        val target = deleteTarget ?: return
        viewModelScope.launch {
            books.deleteGroup(target.id)
            deleteTarget = null
        }
    }

    fun startPickGroup(book: BookRow) {
        pickTarget = book
    }

    fun dismissPickGroup() {
        pickTarget = null
    }

    fun setBookGroup(bookName: String, groupId: Long?) {
        viewModelScope.launch {
            books.setBookGroup(bookName, groupId)
            pickTarget = null
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { BookManageViewModel(container.books) }
        }
    }
}
