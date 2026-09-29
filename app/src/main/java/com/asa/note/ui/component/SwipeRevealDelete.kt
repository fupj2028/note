package com.asa.note.ui.component

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RevealWidth = 88.dp

/**
 * 左滑露出删除按钮，再点一下才真删 —— 两步式，不做"滑过去直接删"，免得误删。
 *
 * [content] 会拿到一个 close 回调，请在内容自己的点击里调它（一般是 onClick = close）：
 * 这样展开后点一下就能收起，**同时右滑也能拖回去**。
 * 不要改成在内容上盖一层透明的"点击收起"层 —— 那层会把拖拽事件一并吃掉，就再也划不回去了。
 */
@Composable
fun SwipeRevealDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    contentBackground: Color = MaterialTheme.colorScheme.surface,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val revealPx = with(LocalDensity.current) { RevealWidth.toPx() }
    val scope = rememberCoroutineScope()
    var offset by remember { mutableFloatStateOf(0f) }
    val close: () -> Unit = {
        scope.launch { animate(offset, 0f) { value, _ -> offset = value } }
    }

    Box(modifier) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.errorContainer),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    close()
                    onDelete()
                },
                modifier = Modifier.width(RevealWidth),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(-revealPx, 0f)
                    },
                    onDragStopped = {
                        val target = if (offset < -revealPx * 0.5f) -revealPx else 0f
                        scope.launch { animate(offset, target) { value, _ -> offset = value } }
                    },
                )
                .background(contentBackground),
        ) {
            content(close)
        }
    }
}
