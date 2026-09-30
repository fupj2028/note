package com.asa.note.ui.book

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.asa.note.data.BookGroupRow
import com.asa.note.data.BookRow
import com.asa.note.repo.NameResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManageScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: BookManageViewModel = viewModel(factory = BookManageViewModel.factory(container))
    val groupRows by vm.groupRows.collectAsStateWithLifecycle()
    val bookRows by vm.bookRows.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("书目管理") },
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
            Text(
                text = "书是自动出现的（摘录里填了书名就会有），你只需要给它们挑个大类。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )

            if (groupRows.isEmpty() && bookRows.isEmpty()) {
                Text(
                    text = "还没有书。先在摘录里填上书名，这里就会出现。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
                return@Column
            }

            SectionHead("大类")
            HorizontalDivider()
            groupRows.forEach { row ->
                GroupRowView(
                    row = row,
                    onRename = { vm.startRenameGroup(row) },
                    onDelete = { vm.askDeleteGroup(row) },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = vm::startCreateGroup)
                    .padding(vertical = 14.dp),
            ) {
                Text(
                    text = "＋ 新建大类",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SectionHead("书")
            HorizontalDivider()
            val groupNames = groupRows.associate { it.id to it.name }
            bookRows.forEach { book ->
                BookRowView(
                    row = book,
                    groupName = groupNames[book.groupId],
                    onPick = { vm.startPickGroup(book) },
                )
            }
            Box(Modifier.padding(bottom = 28.dp))
        }
    }

    vm.pickTarget?.let { book ->
        AlertDialog(
            onDismissRequest = vm::dismissPickGroup,
            title = { Text("把「${book.name}」归到哪个大类") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ChoiceRow("未归类", book.groupId == null) { vm.setBookGroup(book.name, null) }
                    groupRows.forEach { group ->
                        ChoiceRow(group.name, book.groupId == group.id) {
                            vm.setBookGroup(book.name, group.id)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.startCreateGroupForBook(book) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "＋ 新建大类…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::dismissPickGroup) { Text("取消") }
            },
        )
    }

    vm.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = vm::dismissDeleteGroup,
            title = { Text("删掉大类「${target.name}」？") },
            text = {
                Text(
                    if (target.bookCount == 0) {
                        "这个大底下没有书。删掉之后书和摘录都不受影响。"
                    } else {
                        "里面有 ${target.bookCount} 本书、${target.excerptCount} 条摘录。删掉大类后这些书会变成「未归类」，书和摘录都不会删。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmDeleteGroup) { Text("删掉大类") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDeleteGroup) { Text("取消") }
            },
        )
    }

    vm.nameDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = vm::dismissNameDialog,
            title = { Text(if (dialog.id == null) "新建大类" else "重命名大类") },
            text = {
                Column {
                    OutlinedTextField(
                        value = dialog.text,
                        onValueChange = vm::onNameChange,
                        placeholder = { Text("大类名，例如 文学 / 历史") },
                        singleLine = true,
                        isError = dialog.error != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    dialog.error?.let { error ->
                        Text(
                            text = when (error) {
                                NameResult.BlankName -> "名字不能空着"
                                NameResult.DuplicateName -> "已经有同名的大类了"
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
                TextButton(onClick = vm::submitName, enabled = dialog.text.isNotBlank()) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissNameDialog) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SectionHead(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun GroupRowView(row: BookGroupRow, onRename: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${row.bookCount} 本书 · ${row.excerptCount} 条摘录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = "重命名",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onRename).padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Text(
            text = "删除",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable(onClick = onDelete).padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
    HorizontalDivider()
}

@Composable
private fun BookRowView(row: BookRow, groupName: String?, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${row.excerptCount} 条摘录",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = if (groupName == null) "未归类 ›" else "$groupName ›",
            style = MaterialTheme.typography.labelMedium,
            color = if (groupName == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
    HorizontalDivider()
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
