package app.phayzee.offline_first_arch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the app.
 * Single source of truth for all local data.
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "offline_notes.db"
    }
}