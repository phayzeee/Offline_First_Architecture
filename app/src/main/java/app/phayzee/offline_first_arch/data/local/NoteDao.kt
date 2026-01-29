package app.phayzee.offline_first_arch.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Note operations.
 * Room will generate the implementation.
 */
@Dao
interface NoteDao {

    /**
     * Observe all notes excluding those marked for deletion.
     * Returns a Flow for reactive UI updates.
     */
    @Query("SELECT * FROM notes WHERE syncState != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    /**
     * Get all notes that need to be synced.
     */
    @Query("SELECT * FROM notes WHERE syncState != 'SYNCED'")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    /**
     * Count of notes pending sync - useful for UI indicator.
     */
    @Query("SELECT COUNT(*) FROM notes WHERE syncState != 'SYNCED'")
    fun observePendingCount(): Flow<Int>

    /**
     * Get a single note by ID.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity?

    /**
     * Observe a single note by ID.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNoteById(id: String): Flow<NoteEntity?>

    /**
     * Insert a new note. Replace on conflict (upsert behavior).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    /**
     * Insert multiple notes (for batch sync operations).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    /**
     * Update an existing note.
     */
    @Update
    suspend fun update(note: NoteEntity)

    /**
     * Hard delete a note from local database.
     * Only called after successful remote deletion.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Mark a note's sync state.
     */
    @Query("UPDATE notes SET syncState = :syncState WHERE id = :id")
    suspend fun updateSyncState(id: String, syncState: String)

    /**
     * Clear all notes (for testing/logout).
     */
    @Query("DELETE FROM notes")
    suspend fun clearAll()
}
