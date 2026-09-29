package com.asa.note.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer
import com.asa.note.data.entity.CategoryEntity
import com.asa.note.util.showExportResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    container: AppContainer,
    noteId: Long,
    onBack: () -> Unit,
) {
    val vm: NoteEditorViewModel = viewModel(
        factory = NoteEditorViewModel.factory(container, noteId),
        key = "editor-$noteId",
    )
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val allCategories by vm.allCategories.collectAsStateWithLifecycle()

    val leave: () -> Unit = {
        scope.launch {
            vm.flush()
            onBack()
        }
    }

    BackHandler(onBack = leave)

    LaunchedEffect(vm.trashed) {
        if (vm.trashed) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Text(
                        text = when {
                            vm.saving -> "正在保存…"
                            vm.savedOnce -> "已保存"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            enabled = vm.canManage,
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (vm.isPinned) "取消置顶" else "置顶") },
                                onClick = {
                                    menuOpen = false
                                    vm.togglePin()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("分类") },
                                onClick = {
                                    menuOpen = false
                                    vm.openCategoryPicker()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("导出这条") },
                                onClick = {
                                    menuOpen = false
                                    scope.launch {
                                        vm.flush()
                                        showExportResult(context, container.export.exportNote(vm.savedId))
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
                .padding(horizontal = 16.dp),
        ) {
            BasicTextField(
                value = vm.title,
                onValueChange = vm::onTitleChange,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            )
            HorizontalDivider()
            BasicTextField(
                value = vm.content,
                onValueChange = vm::onContentChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
            )
        }
    }

    if (vm.pickingCategory) {
        CategoryPickerDialog(
            categories = allCategories,
            selectedId = vm.currentCategoryId,
            onPick = vm::setCategory,
            onDismiss = vm::dismissCategoryPicker,
        )
    }
}

/**
 * 归类入口。原设计放在 ④ 长按面板里，但 ④ 已改成"长按直接复制"，
 * 这个入口就挪到编辑页的「⋮」——否则分类能建却没法用。
 */
@Composable
private fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onPick: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("归到哪个分类") },
        text = {
            Column {
                if (categories.isEmpty()) {
                    Text(
                        text = "还没有分类。去「设置 → 分类管理」建一个再来。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    CategoryChoice("未分类", selectedId == null) { onPick(null) }
                    categories.forEach { category ->
                        CategoryChoice(
                            label = if (category.isHidden) {
                                "${category.name}（已隐藏）"
                            } else {
                                category.name
                            },
                            selected = selectedId == category.id,
                            onClick = { onPick(category.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun CategoryChoice(label: String, selected: Boolean, onClick: () -> Unit) {
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
