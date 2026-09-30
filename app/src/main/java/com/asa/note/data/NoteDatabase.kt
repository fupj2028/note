package com.asa.note.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.asa.note.data.entity.BookEntity
import com.asa.note.data.entity.BookGroupEntity
import com.asa.note.data.entity.CategoryEntity
import com.asa.note.data.entity.ExcerptCommentEntity
import com.asa.note.data.entity.ExcerptEntity
import com.asa.note.data.entity.NoteEntity

@Database(
    entities = [
        NoteEntity::class,
        CategoryEntity::class,
        ExcerptEntity::class,
        ExcerptCommentEntity::class,
        BookGroupEntity::class,
        BookEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun excerptDao(): ExcerptDao
    abstract fun bookDao(): BookDao
}
