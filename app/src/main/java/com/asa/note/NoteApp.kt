package com.asa.note

import android.app.Application
import androidx.room.Room
import com.asa.note.data.NoteDatabase
import com.asa.note.data.MIGRATION_1_2
import com.asa.note.repo.BookRepository
import com.asa.note.repo.CategoryRepository
import com.asa.note.repo.ExcerptRepository
import com.asa.note.repo.ExportRepository
import com.asa.note.repo.NoteRepository
import com.asa.note.repo.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NoteApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: NoteDatabase = Room
        .databaseBuilder(application, NoteDatabase::class.java, "note.db")
        // 已发布的 v0.1.0 装过真机、有真实数据，所以必须走迁移，不能靠卸载重装。
        .addMigrations(MIGRATION_1_2)
        .build()

    val notes = NoteRepository(database.noteDao())
    val categories = CategoryRepository(database.categoryDao())
    val excerpts = ExcerptRepository(database.excerptDao())
    val books = BookRepository(database.bookDao())
    val settings = SettingsRepository(application)
    val export = ExportRepository(application, notes, categories, excerpts)

    init {
        scope.launch {
            notes.purgeExpired()
            excerpts.purgeExpired()
        }
    }
}
