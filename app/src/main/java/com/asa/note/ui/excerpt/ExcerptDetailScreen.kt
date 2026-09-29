package com.asa.note.ui.excerpt

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.ui.component.SwipeRevealDelete
import com.asa.note.util.TimeText
import com.asa.note.util.quickCopy
import com.asa.note.util.showExportResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcerptDetailScreen(
    container: AppContainer,
    excerptId: Long,
    onBack: () -> Unit,
) {
    val vm: ExcerptDetailViewModel = viewModel(
        factory = ExcerptDetailViewModel.factory(container, excerptId),
        key = "excerpt-$excerptId",
    )
    val comments by vm.comments.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(vm.trashed) {
        if (vm.trashed) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("摘录") },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("导出这条") },
                                onClick = {
                                    menuOpen = false
                                    scope.launch {
                                        showExportResult(context, container.export.exportExcerpt(excerptId))
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("移到回收站") },
                                onClick = {
                                    menuOpen = false
                                    vm.moveToTrash()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            val excerpt = vm.excerpt
            if (excerpt != null) {
                // 详情页只复制正文本身；要看"原文 + 出处"一起去流页面长按那条。
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(top = 8.dp)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                quickCopy(context, haptics, excerpt.text, "excerpt")
                            },
                        ),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline),
                    )
                    Text(
                        text = excerpt.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(
                    text = buildString {
                        if (excerpt.source.isNotBlank()) {
                            append(excerpt.source)
                            append(" · ")
                        }
                        append(TimeText.stamp(excerpt.createdAt))
                        append(" 收录")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Text(
                text = "我的评论 · ${comments.size} 条",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
            )

            if (comments.isEmpty()) {
                Text(
                    text = "还没写评论",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                comments.forEach { comment ->
                    CommentRow(
                        comment = comment,
                        onCopy = {
                            quickCopy(context, haptics, comment.text, "excerpt-comment")
                        },
                        onDelete = { vm.deleteComment(comment.id) },
                    )
                }
            }

            Text(
                text = "追加一条",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
            )
            OutlinedTextField(
                value = vm.draft,
                onValueChange = vm::onDraftChange,
                placeholder = { Text("写点什么…") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
                Box(Modifier.weight(1f))
                Button(
                    onClick = vm::addComment,
                    enabled = vm.draft.isNotBlank(),
                ) { Text("发出去") }
            }

            Text(
                text = "原文不能改。长按正文或某条评论，单独复制那一段；评论左滑可以删掉。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 28.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentRow(
    comment: ExcerptCommentEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    SwipeRevealDelete(
        onDelete = onDelete,
        contentBackground = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
    ) { close ->
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .combinedClickable(onClick = close, onLongClick = onCopy)
                .padding(vertical = 4.dp),
        ) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Column(Modifier.padding(start = 10.dp)) {
                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = TimeText.stamp(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
