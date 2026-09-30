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
import com.asa.note.repo.BookRepository
import com.asa.note.repo.ExcerptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExcerptComposerViewModel(
    private val excerpts: ExcerptRepository,
    private val books: BookRepository,
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
    var pickingGroup by mutableStateOf(false)
        private set
    var saved by mutableStateOf(false)
        private set

    /** 用户在面板里挑过哪个大类。没挑过就不写库，这本书原来的归属不动。 */
    private var pickedGroupId by mutableStateOf<Long?>(null)
    private var groupPicked by mutableStateOf(false)

    val knownSources: StateFlow<List<String>> = excerpts.observeSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups: StateFlow<List<BookGroupEntity>> = books.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 书名 → 它现在归的大类：显示用，所以要主动订阅，不能等到界面来收才加载。 */
    val bookRows: StateFlow<List<BookRow>> = books.observeBooks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * 界面上显示的大类 id：刚点进来（书名是从筛选带过来的）就先亮出这本书现在的大类，
     * 用户自己挑过之后以挑的为准。书列表由调用方从 [bookRows] 收上来，好让界面跟着刷新。
     */
    fun shownGroupId(bookRows: List<BookRow>): Long? = if (groupPicked) {
        pickedGroupId
    } else {
        bookRows.firstOrNull { it.name == source.trim() }?.groupId
    }

    fun onTextChange(value: String) {
        text = value
    }

    /** 改了书名，分类跟着换书名走（以那本书现在的大类为准），免得归错。 */
    fun onSourceChange(value: String) {
        source = value
        groupPicked = false
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
        groupPicked = false
        pickingSource = false
    }

    fun openGroupPicker() {
        pickingGroup = true
    }

    fun dismissGroupPicker() {
        pickingGroup = false
    }

    fun pickGroup(id: Long?) {
        pickedGroupId = id
        groupPicked = true
        pickingGroup = false
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
            // 只有用户主动挑过大类才回写到 book 表；没挑的话这本书原来归哪儿就还归哪儿。
            if (groupPicked && source.isNotBlank()) {
                books.setBookGroup(source.trim(), pickedGroupId)
            }
            saved = true
        }
    }

    companion object {
        fun factory(container: AppContainer, initialSource: String) = viewModelFactory {
            initializer {
                ExcerptComposerViewModel(container.excerpts, container.books, initialSource)
            }
        }
    }
}
