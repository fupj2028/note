package com.asa.note.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.data.entity.NoteEntity
import com.asa.note.ui.component.EmptyView
import com.asa.note.util.TimeText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenExcerpt: (Long) -> Unit,
) {
    val vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(container))
    val notes by vm.noteHits.collectAsStateWithLifecycle()
    val excerpts by vm.excerptHits.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出搜索")
                    }
                },
                title = {
                    TextField(
                        value = vm.query,
                        onValueChange = vm::updateQuery,
                        placeholder = { Text("搜备忘录和摘录") },
                        singleLine = true,
                        modifier = Modifier.focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                },
                actions = {
                    if (vm.query.isNotEmpty()) {
                        IconButton(onClick = { vm.updateQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            when {
                vm.query.isBlank() -> EmptyView(
                    title = "搜点什么",
                    hint = "备忘录的标题正文、摘录的原文书名和评论，都能搜到",
                )

                notes.isEmpty() && excerpts.isEmpty() -> EmptyView(
                    title = "没搜到「${vm.query}」",
                    hint = "换个词试试",
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                ) {
                    if (notes.isNotEmpty()) {
                        item(key = "h-note") { ResultGroupHeader("备忘录 · ${notes.size} 条") }
                        items(notes, key = { "n-${it.id}" }) { note ->
                            NoteHit(note, vm.query) { onOpenNote(note.id) }
                        }
                    }
                    if (excerpts.isNotEmpty()) {
                        item(key = "h-excerpt") { ResultGroupHeader("摘录 · ${excerpts.size} 条") }
                        items(excerpts, key = { "e-${it.id}" }) { excerpt ->
                            ExcerptHit(excerpt, vm.query) { onOpenExcerpt(excerpt.id) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultGroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun NoteHit(note: NoteEntity, query: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Highlighted(note.title.ifBlank { "（无标题）" }, query, MaterialTheme.typography.titleMedium)
        if (note.content.isNotBlank()) {
            Highlighted(
                text = note.content,
                query = query,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Box(Modifier.weight(1f))
            Text(
                text = TimeText.listTime(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExcerptHit(excerpt: ExcerptEntity, query: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Highlighted(excerpt.text, query, MaterialTheme.typography.bodyMedium)
        Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
            if (excerpt.source.isNotBlank()) {
                Text(
                    text = excerpt.source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.weight(1f))
            Text(
                text = TimeText.listTime(excerpt.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 命中的词打底色加粗。大小写不敏感，与 SQL 那边 LIKE 的行为对齐。 */
@Composable
private fun Highlighted(
    text: String,
    query: String,
    style: TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
) {
    val highlight = MaterialTheme.colorScheme.primaryContainer
    val rendered: AnnotatedString = remember(text, query, highlight) {
        if (query.isBlank()) return@remember AnnotatedString(text)
        buildAnnotatedString {
            var cursor = 0
            var index = text.indexOf(query, cursor, ignoreCase = true)
            if (index < 0) {
                append(text)
                return@buildAnnotatedString
            }
            while (index >= 0) {
                append(text.substring(cursor, index))
                withStyle(SpanStyle(background = highlight, fontWeight = FontWeight.SemiBold)) {
                    append(text.substring(index, index + query.length))
                }
                cursor = index + query.length
                index = text.indexOf(query, cursor, ignoreCase = true)
            }
            append(text.substring(cursor))
        }
    }

    Text(
        text = rendered,
        style = style,
        color = color,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
