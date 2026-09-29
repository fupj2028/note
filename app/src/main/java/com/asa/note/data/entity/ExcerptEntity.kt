package com.asa.note.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "excerpt",
    indices = [Index("createdAt"), Index("source"), Index("deletedAt")],
)
data class ExcerptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val source: String = "",
    /** 收录时刻。只追加，写一次不再改。 */
    val createdAt: Long,
    val deletedAt: Long? = null,
)
