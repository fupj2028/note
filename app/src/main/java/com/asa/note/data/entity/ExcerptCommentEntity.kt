package com.asa.note.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 只有自己看，所以没有作者字段、没有回复关系，评论是平铺的一串。 */
@Entity(
    tableName = "excerpt_comment",
    foreignKeys = [
        ForeignKey(
            entity = ExcerptEntity::class,
            parentColumns = ["id"],
            childColumns = ["excerptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("excerptId")],
)
data class ExcerptCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val excerptId: Long,
    val text: String,
    val createdAt: Long,
)
