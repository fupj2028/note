package com.asa.note.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 → 2：新增「大类」和「书」两张表，给摘录做两级分类。
 *
 * **现有数据一行都不动** —— 只建两张空表：摘录仍然用 `source` 字符串记书名，
 * 书的大类是之后在「书目管理」里分配的。所以这个迁移是安全的、可重入的。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `book_group` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_book_group_name` " +
                "ON `book_group` (`name`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `book` " +
                "(`name` TEXT NOT NULL, `groupId` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`name`), " +
                "FOREIGN KEY(`groupId`) REFERENCES `book_group`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_book_groupId` ON `book` (`groupId`)",
        )
    }
}
