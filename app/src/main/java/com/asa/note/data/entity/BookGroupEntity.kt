package com.asa.note.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 摘录的「大类」，例如 文学 / 历史。跟备忘录的「分类」是两套东西，互不共用。 */
@Entity(
    tableName = "book_group",
    indices = [Index(value = ["name"], unique = true)],
)
data class BookGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
)
