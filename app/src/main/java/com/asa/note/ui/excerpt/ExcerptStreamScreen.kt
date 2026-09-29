package com.asa.note.ui.excerpt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.ui.component.EmptyView
import com.asa.note.util.TimeText
import com.asa.note.util.quickCopy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcerptStreamScreen(
    container: AppContainer,
    onOpenDetail: (Long) -> Unit,
    onOpenComposer: (String?) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val vm: ExcerptStreamViewModel = viewModel(factory = ExcerptStreamViewModel.factory(container))
    val stream by vm.stream.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val comments by vm.commentsByExcerpt.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Scaffold(
        // 同列表页：底部留白由外层 HomeScreen 的底部栏负责，这里只要状态栏那一份。
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("摘录") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenComposer(vm.selectedSource) }) {
                Icon(Icons.Filled.Add, contentDescription = "写摘录")
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SourceChips(sources, vm.selectedSource, vm::selectSource)

            if (stream.isEmpty()) {
                EmptyView("还没有摘录", "点右下角，把书上的句子粘进来")
            } else {
                Stream(
                    items = stream,
                    comments = comments,
                    onOpenDetail = onOpenDetail,
                    onLongPress = { excerpt ->
                        quickCopy(context, haptics, excerptPayload(excerpt), "excerpt")
                    },
                )
            }
        }
    }
}

/** 长按直接复制：原文，带出处的时候连出处一起给。 */
private fun excerptPayload(excerpt: ExcerptEntity): String = buildString {
    append(excerpt.text)
    if (excerpt.source.isNotBlank()) {
        append("\n—— ")
        append(excerpt.source)
    }
}

@Composable
private fun SourceChips(
    sources: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("全部") },
            )
        }
        items(sources, key = { it }) { source ->
            FilterChip(
                selected = selected == source,
                onClick = { onSelect(source) },
                label = { Text(source) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Stream(
    items: List<ExcerptEntity>,
    comments: Map<Long, List<ExcerptCommentEntity>>,
    onOpenDetail: (Long) -> Unit,
    onLongPress: (ExcerptEntity) -> Unit,
) {
    val byDay = items.groupBy { TimeText.dayKey(it.createdAt) }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        byDay.forEach { (_, dayItems) ->
            item(key = "day-${dayItems.first().id}") {
                Text(
                    text = TimeText.dayHeader(dayItems.first().createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                )
            }
            items(dayItems, key = { "ex-${it.id}" }) { excerpt ->
                ExcerptCard(
                    excerpt = excerpt,
                    comments = comments[excerpt.id].orEmpty(),
                    onOpen = { onOpenDetail(excerpt.id) },
                    onLongPress = { onLongPress(excerpt) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExcerptCard(
    excerpt: ExcerptEntity,
    comments: List<ExcerptCommentEntity>,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(excerpt.text, style = MaterialTheme.typography.bodyLarge)

            if (comments.isNotEmpty()) {
                CommentPreview(comments)
            }

            // 出处和时间放卡片最底部，跟备忘录的卡片一致；评论多了也不会把时间挤到中间。
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (excerpt.source.isNotBlank()) {
                    Text(
                        text = excerpt.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.weight(1f))
                Text(
                    text = TimeText.timeOnly(excerpt.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentPreview(comments: List<ExcerptCommentEntity>) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Column(Modifier.padding(start = 10.dp)) {
            comments.take(2).forEach { comment ->
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (comments.size > 2) {
                Text(
                    text = "还有 ${comments.size - 2} 条 ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
