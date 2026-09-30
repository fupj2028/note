package com.asa.note.ui.excerpt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.R
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.ui.component.EmptyView
import com.asa.note.util.TimeText
import com.asa.note.util.quickCopy

/** 两种列表的卡片都用这个高度，固定住、超出截断；完整内容点进详情页看。 */
private val CardHeight = 112.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcerptStreamScreen(
    container: AppContainer,
    onOpenDetail: (Long) -> Unit,
    onOpenComposer: (String?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookManage: () -> Unit,
) {
    val vm: ExcerptStreamViewModel = viewModel(factory = ExcerptStreamViewModel.factory(container))
    val stream by vm.stream.collectAsStateWithLifecycle()
    val groups by vm.groups.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val comments by vm.commentsByExcerpt.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val groupName = groups.firstOrNull { it.id == vm.filter.groupId }?.name
    val filterLabel = when {
        vm.filter.source != null && groupName != null -> "$groupName · ${vm.filter.source}"
        vm.filter.source != null -> vm.filter.source.orEmpty()
        groupName != null -> groupName
        else -> "全部书目"
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("摘录") },
                actions = {
                    FilterPill(
                        label = filterLabel,
                        active = vm.filter.isActive,
                        onOpen = vm::openPicker,
                        onClear = if (vm.filter.isActive) vm::clearFilter else null,
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onOpenComposer(vm.filter.source) }) {
                Icon(Icons.Filled.Add, contentDescription = "写摘录")
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (stream.isEmpty()) {
                if (vm.filter.isActive) {
                    EmptyView("这个范围里没有摘录", "换一个书目，或者点上面的胶囊清除筛选")
                } else {
                    EmptyView("还没有摘录", "点右下角，把书上的句子粘进来")
                }
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

    if (vm.picking) {
        BookPickerSheet(
            groups = groups.map { it.id to it.name },
            books = books.map { it.name to it.groupId },
            current = vm.filter,
            onPick = vm::applyFilter,
            onClearAll = vm::clearFilter,
            onOpenBookManage = {
                vm.dismissPicker()
                onOpenBookManage()
            },
            onDismiss = vm::dismissPicker,
        )
    }
}

/** 顶栏那个筛选胶囊：平时显示「全部书目」，筛了之后高亮并带一个 ✕ 一键回全部。 */
@Composable
private fun FilterPill(
    label: String,
    active: Boolean,
    onOpen: () -> Unit,
    onClear: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(shape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .border(
                width = 1.dp,
                color = if (active) Color.Transparent else MaterialTheme.colorScheme.outline,
                shape = shape,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(start = 12.dp, end = if (onClear == null) 10.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 132.dp),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        if (onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "清除筛选",
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** 两级选择面板：左栏窄（大类），右栏宽（书）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookPickerSheet(
    groups: List<Pair<Long, String>>,
    books: List<Pair<String, Long?>>,
    current: BookFilter,
    onPick: (Long?, String?) -> Unit,
    onClearAll: () -> Unit,
    onOpenBookManage: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingGroup by remember { mutableStateOf(current.groupId) }
    val visibleBooks = books.filter { pendingGroup == null || it.second == pendingGroup }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(Modifier.padding(bottom = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "按书目筛选",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                // 管理入口放这儿（原来在面板最底部，太隐蔽）；清除筛选只清条件、不删数据，所以用图标并写明描述。
                TextButton(onClick = onOpenBookManage) { Text("管理书目 ›") }
                IconButton(onClick = onClearAll) {
                    Icon(
                        painter = painterResource(R.drawable.ic_filter_off),
                        contentDescription = "清除筛选",
                    )
                }
            }
            HorizontalDivider()

            Row(Modifier.fillMaxWidth().height(304.dp)) {
                Column(
                    modifier = Modifier
                        .width(118.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PickerRow(
                        label = "全部",
                        selected = pendingGroup == null,
                        onClick = {
                            pendingGroup = null
                            onPick(null, null)
                        },
                    )
                    groups.forEach { (id, name) ->
                        PickerRow(
                            label = name,
                            selected = pendingGroup == id,
                            onClick = {
                                pendingGroup = id
                                onPick(id, null)
                            },
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                ) {
                    PickerRow(
                        label = "全部",
                        selected = current.source == null,
                        onClick = { onPick(pendingGroup, null) },
                    )
                    visibleBooks.forEach { (name, _) ->
                        PickerRow(
                            label = name,
                            selected = current.source == name,
                            onClick = { onPick(pendingGroup, name) },
                        )
                    }
                }
            }

            HorizontalDivider()
        }
    }
}

/** 选中项整行上主题色底（跟侧边抽屉选中项一个做法），比只在最右侧打个勾好认。 */
@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else null,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor,
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Stream(
    items: List<ExcerptEntity>,
    comments: Map<Long, List<ExcerptCommentEntity>>,
    onOpenDetail: (Long) -> Unit,
    onLongPress: (ExcerptEntity) -> Unit,
) {
    val byDay = items.groupBy { TimeText.dayKey(it.createdAt) }
    val listState = rememberLazyListState()

    // 摘录是从旧到新排（像聊天记录），所以进来要停在最底下那条。
    // 用"最新一条的 id"当触发条件：首次加载、以及新加了一条时都会跳到最新的那条；
    // 光靠 items.size 会在删除时也乱跳。
    val newestId = items.lastOrNull()?.id
    val totalItems = byDay.size + items.size
    LaunchedEffect(newestId, totalItems) {
        if (totalItems > 0) listState.scrollToItem(totalItems - 1)
    }

    LazyColumn(
        state = listState,
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
                    commentCount = comments[excerpt.id].orEmpty().size,
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
    commentCount: Int,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = excerpt.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            // 出处和时间贴卡片底部；评论只报条数，全文进详情页看 —— 这样高度才定得住。
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (excerpt.source.isNotBlank()) {
                    Text(
                        text = excerpt.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (commentCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$commentCount 条评论",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
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
