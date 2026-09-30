package com.asa.note.repo

/** 新建 / 重命名时的命名校验结果。备忘包的「分类」和摘录的「大类」共用。 */
sealed interface NameResult {
    data object Ok : NameResult
    data object BlankName : NameResult
    data object DuplicateName : NameResult
}
