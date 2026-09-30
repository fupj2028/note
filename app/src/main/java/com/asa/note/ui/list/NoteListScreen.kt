package com.asa.note.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.R
import com.asa.note.data.entity.CategoryEntity
import com.asa.note.data.entity.NoteEntity
import com.asa.note.ui.component.EmptyView
import com.asa.note.util.TimeText
import com.asa.note.util.quickCopy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    container: AppContainer,
    onOpenEditor: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
) {
    val vm: NoteListViewModel = viewModel(factory = NoteListViewModel.factory(container))
    val notes by vm.visibleNotes.collectAsStateWithLifecycle()
    val categories by vm.visibleCategories.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }

    Scaffold(
        // 外层 HomeScreen 已经把底部栏占位算进 innerPadding 了，这里只要状态栏那一份，
        // 否则底部会多出一段空白。
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("备忘录") },
                actions = {
                    // 分类管理放顶栏右侧，且用图标而不是 chip —— 否则跟分类本身长得一样，分不出来。
                    IconButton(onClick = onOpenCategories) {
                        Icon(
                            painter = painterResource(R.drawable.ic_category_tag),
                            contentDescription = "分类管理",
                        )
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "设置")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenEditor(0L) }) {
                Icon(Icons.Filled.Add, contentDescription = "新建笔记")
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            CategoryChips(
                categories = categories,
                selectedId = vm.selectedCategoryId,
                onSelect = vm::selectCategory,
            )

            if (notes.isEmpty()) {
                EmptyView("还没有笔记", "点右下角的加号写第一条")
            } else {
                NoteList(
                    notes = notes,
                    categoryNames = categoryNames,
                    onOpen = { onOpenEditor(it.id) },
                    onLongPress = { note ->
                        quickCopy(context, haptics, notePayload(note), "note")
                    },
                )
            }
        }
    }
}

/** 长按直接复制整条，标题和正文一起给。置顶、删除在编辑页的「⋮」里。 */
private fun notePayload(note: NoteEntity): String = buildString {
    if (note.title.isNotBlank()) {
        append(note.title)
        append('\n')
    }
    append(note.content)
}

@Composable
private fun CategoryChips(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text("全部") },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteList(
    notes: List<NoteEntity>,
    categoryNames: Map<Long, String>,
    onOpen: (NoteEntity) -> Unit,
    onLongPress: (NoteEntity) -> Unit,
) {
    val pinned = notes.filter { it.isPinned }
    val others = notes.filterNot { it.isPinned }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (pinned.isNotEmpty()) {
            item(key = "h-pinned") { SectionHeader("置顶") }
            items(pinned, key = { "p-${it.id}" }) { note ->
                NoteCard(note, categoryNames[note.categoryId], onOpen, onLongPress)
            }
        }
        item(key = "h-all") {
            SectionHeader(if (others.isEmpty()) "没有其它笔记" else "全部 · ${others.size} 条")
        }
        items(others, key = { "n-${it.id}" }) { note ->
            NoteCard(note, categoryNames[note.categoryId], onOpen, onLongPress)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
    )
}

/** 两种列表的卡片都用这个高度，固定住、超出截断；完整内容点进详情页看。 */
private val CardHeight = 112.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: NoteEntity,
    categoryName: String?,
    onOpen: (NoteEntity) -> Unit,
    onLongPress: (NoteEntity) -> Unit,
) {
    // 非置顶用 Card 的默认底色，跟摘录卡片对齐；原来写死 surface，所以两个 tab 的卡片底色不一样。
    Card(
        colors = if (note.isPinned) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .combinedClickable(
                onClick = { onOpen(note) },
                onLongClick = { onLongPress(note) },
            ),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = note.title.ifBlank { "（无标题）" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (categoryName != null) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.weight(1f))
                Text(
                    text = TimeText.listTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
