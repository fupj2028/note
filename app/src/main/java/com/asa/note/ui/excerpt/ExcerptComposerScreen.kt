package com.asa.note.ui.excerpt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asa.note.AppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcerptComposerScreen(
    container: AppContainer,
    initialSource: String,
    onDone: () -> Unit,
) {
    val vm: ExcerptComposerViewModel = viewModel(
        factory = ExcerptComposerViewModel.factory(container, initialSource),
        key = "excerpt-composer",
    )
    val knownSources by vm.knownSources.collectAsStateWithLifecycle()

    LaunchedEffect(vm.saved) {
        if (vm.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Close, contentDescription = "放弃")
                    }
                },
                title = { Text("写摘录") },
                actions = {
                    TextButton(onClick = vm::save, enabled = vm.text.isNotBlank()) {
                        Text("保存")
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
            FieldLabel("原文（粘贴或手打）")
            OutlinedTextField(
                value = vm.text,
                onValueChange = vm::onTextChange,
                placeholder = { Text("把书上的句子粘进来") },
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )

            FieldLabel("出处")
            OutlinedTextField(
                value = vm.source,
                onValueChange = vm::onSourceChange,
                placeholder = { Text("书名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { vm.openSourcePicker() },
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text("从摘过的书里挑")
            }
            Text(
                text = "也可以直接手打书名",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FieldLabel("我的评论（可不写）")
            OutlinedTextField(
                value = vm.comment,
                onValueChange = vm::onCommentChange,
                placeholder = { Text("这块和原文分开存，之后还能接着追加") },
                modifier = Modifier.fillMaxWidth().height(110.dp),
            )

            Text(
                text = "收录时间自动记为「现在」，存了就不能改。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
            )
        }
    }

    if (vm.pickingSource) {
        AlertDialog(
            onDismissRequest = vm::dismissSourcePicker,
            title = { Text("摘过的书") },
            text = {
                if (knownSources.isEmpty()) {
                    Text("还没有摘过的书，直接手打书名就行")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        knownSources.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.pickSource(name) }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::dismissSourcePicker) { Text("取消") }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Box(Modifier.padding(top = 18.dp, bottom = 6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
