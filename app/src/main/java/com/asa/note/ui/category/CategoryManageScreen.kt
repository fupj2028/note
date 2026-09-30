package com.asa.note.ui.category

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.repo.NameResult
import com.asa.note.ui.component.EmptyView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: CategoryManageViewModel = viewModel(factory = CategoryManageViewModel.factory(container))
    val rows by vm.rows.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("分类管理") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::startCreate) {
                Icon(Icons.Filled.Add, contentDescription = "新建分类")
            }
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (rows.isEmpty()) {
                EmptyView("还没有分类", "点右下角新建一个，或者长按笔记时给它归类")
            } else {
                Text(
                    text = "隐藏的分类不出现在筛选栏，里面的笔记不受影响",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 2.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                ) {
                    items(rows, key = { it.category.id }) { row ->
                        CategoryRowView(
                            row = row,
                            onToggleHidden = { vm.toggleHidden(row) },
                            onRename = { vm.startRename(row) },
                            onDelete = { vm.askDelete(row) },
                        )
                    }
                }
                Text(
                    text = "删掉一个分类，里面的笔记不会被删，只会变成「未分类」；隐藏只是让它从筛选栏里收起来，随时能再放出来。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }

    vm.nameDialog?.let { dialog ->
        NameDialogView(
            dialog = dialog,
            onChange = vm::onNameChange,
            onConfirm = vm::submitName,
            onDismiss = vm::dismissNameDialog,
        )
    }

    vm.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = vm::dismissDelete,
            title = { Text("删掉「${target.category.name}」？") },
            text = {
                Text(
                    if (target.noteCount == 0) {
                        "这个分类下面没有笔记。删掉之后就没了。"
                    } else {
                        "里面有 ${target.noteCount} 条笔记，删掉分类后它们会变成「未分类」，笔记本身不会删。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) { Text("删掉分类") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDelete) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CategoryRowView(
    row: CategoryRow,
    onToggleHidden: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = row.category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(if (row.noteCount == 0) "没有笔记" else "${row.noteCount} 条笔记")
                    if (row.category.isHidden) append(" · 已隐藏")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        RowAction(if (row.category.isHidden) "显示" else "隐藏", onToggleHidden)
        RowAction("重命名", onRename)
        RowAction("删除", onDelete, danger = true)
    }
}

@Composable
private fun RowAction(text: String, onClick: () -> Unit, danger: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (danger) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    )
}

@Composable
private fun NameDialogView(
    dialog: NameDialog,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dialog.id == null) "新建分类" else "重命名分类") },
        text = {
            Column {
                OutlinedTextField(
                    value = dialog.text,
                    onValueChange = onChange,
                    placeholder = { Text("分类名") },
                    singleLine = true,
                    isError = dialog.error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                dialog.error?.let { error ->
                    Text(
                        text = when (error) {
                            NameResult.BlankName -> "名字不能空着"
                            NameResult.DuplicateName -> "已经有同名的分类了"
                            NameResult.Ok -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = dialog.text.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
