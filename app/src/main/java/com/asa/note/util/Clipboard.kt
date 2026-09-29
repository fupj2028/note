package com.asa.note.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/** 长按快速复制：写剪贴板 + 一下震动 + 一句提示，三个一起给，不然用户不确定到底复制上没有。 */
fun quickCopy(context: Context, haptics: HapticFeedback, text: String, label: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}
