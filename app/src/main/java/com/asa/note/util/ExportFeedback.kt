package com.asa.note.util

import android.content.Context
import android.widget.Toast
import com.asa.note.repo.ExportResult

/** 导出没有独立结果页，全部靠这一条提示交代清楚存到哪了。 */
fun showExportResult(context: Context, result: ExportResult) {
    val message = when (result) {
        is ExportResult.Ok -> "已导出到 ${result.location}"
        ExportResult.Failed -> "导出失败，检查一下存储空间"
        ExportResult.Empty -> "没有可导出的内容"
    }
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
