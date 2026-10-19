package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TaskItem::class, CategoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class TimeTrackDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: TimeTrackDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TimeTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TimeTrackDatabase::class.java,
                    "timetrack_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate default customizable categories only.
                // Absolutely NO tasks are pre-populated, keeping schedule completely clean and user-driven.
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        database.categoryDao().insertAll(CategoryItem.defaultCategories())
                    }
                }
            }
        }
    }
}
