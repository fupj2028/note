package com.asa.note.ui.trash

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.ui.component.EmptyView
import com.asa.note.util.TimeText
import java.util.concurrent.TimeUnit

/** 待彻底删的目标。 */
private sealed interface PurgeTarget {
    val label: String

    data class Note(val id: Long, override val label: String) : PurgeTarget
    data class Excerpt(val id: Long, override val label: String) : PurgeTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: TrashViewModel = viewModel(factory = TrashViewModel.factory(container))
    val notes by vm.trashedNotes.collectAsStateWithLifecycle()
    val excerpts by vm.trashedExcerpts.collectAsStateWithLifecycle()

    var purgeTarget by remember { mutableStateOf<PurgeTarget?>(null) }
    var confirmingEmpty by remember { mutableStateOf(false) }
    val total = notes.size + excerpts.size

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("回收站") },
                actions = {
                    if (total > 0) {
                        TextButton(onClick = { confirmingEmpty = true }) { Text("清空") }
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (total == 0) {
                EmptyView("回收站是空的", "删掉的东西先来这里，保留 30 天")
            } else {
                Text(
                    text = "轻点一条＝恢复；保留 30 天，之后自动清理",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 2.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                ) {
                    if (notes.isNotEmpty()) {
                        item(key = "h-note") { GroupHeader("备忘录 · ${notes.size} 条") }
                        items(notes, key = { "n-${it.id}" }) { note ->
                            TrashRow(
                                title = note.title.ifBlank { "（无标题）" },
                                preview = note.content.takeIf { it.isNotBlank() },
                                deletedAt = note.deletedAt,
                                onRestore = { vm.restoreNote(note.id) },
                                onPurge = {
                                    purgeTarget = PurgeTarget.Note(
                                        id = note.id,
                                        label = note.title.ifBlank { "（无标题）" },
                                    )
                                },
                            )
                        }
                    }
                    if (excerpts.isNotEmpty()) {
                        item(key = "h-excerpt") { GroupHeader("摘录 · ${excerpts.size} 条") }
                        items(excerpts, key = { "e-${it.id}" }) { excerpt ->
                            TrashRow(
                                title = excerpt.text,
                                preview = excerpt.source.takeIf { it.isNotBlank() },
                                deletedAt = excerpt.deletedAt,
                                onRestore = { vm.restoreExcerpt(excerpt.id) },
                                onPurge = {
                                    purgeTarget = PurgeTarget.Excerpt(
                                        id = excerpt.id,
                                        label = excerpt.text.take(20),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    purgeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { purgeTarget = null },
            title = { Text("彻底删掉？") },
            text = {
                Text("「${target.label}」从回收站删掉就找不回来了。")
            },
            confirmButton = {
                TextButton(onClick = {
                    when (target) {
                        is PurgeTarget.Note -> vm.purgeNote(target.id)
                        is PurgeTarget.Excerpt -> vm.purgeExcerpt(target.id)
                    }
                    purgeTarget = null
                }) { Text("彻底删掉") }
            },
            dismissButton = {
                TextButton(onClick = { purgeTarget = null }) { Text("取消") }
            },
        )
    }

    if (confirmingEmpty) {
        AlertDialog(
            onDismissRequest = { confirmingEmpty = false },
            title = { Text("清空回收站？") },
            text = { Text("里面的 $total 条都会彻底删掉，找不回来。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.empty()
                    confirmingEmpty = false
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingEmpty = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun TrashRow(
    title: String,
    preview: String?,
    deletedAt: Long?,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (preview != null) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = deletedAt?.let { "${TimeText.stamp(it)} 删除 · ${remaining(it)}" }.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Text(
            text = "彻底删",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clickable(onClick = onPurge)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

private fun remaining(deletedAt: Long): String {
    val daysLeft = RETENTION_DAYS -
        (System.currentTimeMillis() - deletedAt) / TimeUnit.DAYS.toMillis(1)
    return if (daysLeft <= 0) "即将清理" else "还有 $daysLeft 天"
}

private const val RETENTION_DAYS = 30L
