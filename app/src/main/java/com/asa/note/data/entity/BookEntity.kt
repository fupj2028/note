package com.asa.note.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 书。**主键就是书名**（= `excerpt.source`），不做单独 id：
 * 选摘录时填的是书名这个字符串，用字符串当主键就不需要额外映射。
 *
 * 表里只存"这本书归哪个大类"，**书本身从摘录里自动出现**（见 BookDao 的查询），
 * 所以不需要在录入摘录时同步插入书行。
 */
@Entity(
    tableName = "book",
    foreignKeys = [
        ForeignKey(
            entity = BookGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("groupId")],
)
data class BookEntity(
    @PrimaryKey val name: String,
    val groupId: Long? = null,
    val createdAt: Long,
)
