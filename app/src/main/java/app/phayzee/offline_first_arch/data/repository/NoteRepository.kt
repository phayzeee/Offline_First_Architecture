package app.phayzee.offline_first_arch.data.repository


import app.phayzee.offline_first_arch.data.local.NoteDao
import app.phayzee.offline_first_arch.data.local.NoteEntity
import app.phayzee.offline_first_arch.data.remote.FakeRemoteDataSource
import app.phayzee.offline_first_arch.data.sync.NetworkMonitor
import app.phayzee.offline_first_arch.domain.model.Note
import app.phayzee.offline_first_arch.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementing offline-first pattern.
 *
 * Key principles:
 * 1. Local database is the single source of truth
 * 2. All writes go to local first, then sync
 * 3. Remote data refreshes local on sync
 */
@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val remoteDataSource: FakeRemoteDataSource,
    private val networkMonitor: NetworkMonitor
) {
    /**
     * Observe all notes from local database.
     * This is the primary way UI gets note data.
     */
    fun observeNotes(): Flow<List<Note>> =
        noteDao.observeNotes().map { entities ->
            entities.map { it.toDomain() }
        }

    /**
     * Observe a single note by ID.
     */
    fun observeNoteById(id: String): Flow<Note?> =
        noteDao.observeNoteById(id).map { it?.toDomain() }

    /**
     * Observe count of pending changes.
     */
    fun observePendingCount(): Flow<Int> = noteDao.observePendingCount()

    /**
     * Get a single note by ID (one-shot).
     */
    suspend fun getNoteById(id: String): Note? =
        noteDao.getNoteById(id)?.toDomain()

    /**
     * Create a new note.
     * Saves locally immediately, marked for sync.
     */
    suspend fun createNote(title: String, content: String): Note {
        val note = Note.create(title, content)
        noteDao.insert(NoteEntity.fromDomain(note))
        return note
    }

    /**
     * Update an existing note.
     * Updates locally immediately, marked for sync.
     */
    suspend fun updateNote(note: Note, title: String, content: String): Note {
        val updated = note.update(title, content)
        noteDao.update(NoteEntity.fromDomain(updated))
        return updated
    }

    /**
     * Delete a note.
     * Marks for deletion locally, actual removal happens after sync.
     */
    suspend fun deleteNote(note: Note) {
        val marked = note.markForDeletion()
        noteDao.update(NoteEntity.fromDomain(marked))
    }

    /**
     * Perform full sync with remote.
     * Called by WorkManager or manual refresh.
     *
     * Strategy:
     * 1. Push local changes to remote
     * 2. Pull remote changes to local
     *
     * Returns number of items synced or error.
     */
    suspend fun sync(): Result<Int> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.failure(Exception("No network connection"))
        }

        var syncedCount = 0

        // Step 1: Push local pending changes
        val pendingNotes = noteDao.getPendingSyncNotes()
        for (entity in pendingNotes) {
            val note = entity.toDomain()
            val result = when (note.syncState) {
                SyncState.PENDING_CREATE -> pushCreate(note)
                SyncState.PENDING_UPDATE -> pushUpdate(note)
                SyncState.PENDING_DELETE -> pushDelete(note)
                else -> continue
            }

            if (result.isSuccess) {
                syncedCount++
            } else {
                // Mark as failed but continue with other items
                noteDao.updateSyncState(note.id, SyncState.SYNC_FAILED.name)
            }
        }

        // Step 2: Pull remote changes
        // In a real app, you'd use timestamps or versions to only fetch changes
        val remoteResult = remoteDataSource.fetchNotes()
        if (remoteResult.isSuccess) {
            val remoteNotes = remoteResult.getOrThrow()
            // Only update notes that aren't pending local changes
            val localPendingIds = pendingNotes.map { it.id }.toSet()
            remoteNotes
                .filter { it.id !in localPendingIds }
                .forEach { remoteNote ->
                    noteDao.insert(NoteEntity.fromDomain(remoteNote.markSynced()))
                }
        }

        return Result.success(syncedCount)
    }

    /**
     * Initial data load - fetch from remote if online, otherwise use local.
     */
    suspend fun initialLoad(): Result<Unit> {
        if (!networkMonitor.isCurrentlyOnline()) {
            return Result.success(Unit) // Just use local data
        }

        // Seed demo data if server is empty
        remoteDataSource.seedData()

        val result = remoteDataSource.fetchNotes()
        if (result.isSuccess) {
            val notes = result.getOrThrow()
            noteDao.insertAll(notes.map { NoteEntity.fromDomain(it.markSynced()) })
        }
        return result.map { }
    }

    private suspend fun pushCreate(note: Note): Result<Note> {
        return remoteDataSource.createNote(note).onSuccess { synced ->
            noteDao.update(NoteEntity.fromDomain(synced.markSynced()))
        }
    }

    private suspend fun pushUpdate(note: Note): Result<Note> {
        return remoteDataSource.updateNote(note).onSuccess { synced ->
            noteDao.update(NoteEntity.fromDomain(synced.markSynced()))
        }
    }

    private suspend fun pushDelete(note: Note): Result<Unit> {
        return remoteDataSource.deleteNote(note.id).onSuccess {
            noteDao.deleteById(note.id)
        }
    }

    /**
     * Force retry sync for failed items.
     */
    suspend fun retryFailedSync() {
        val failed = noteDao.getPendingSyncNotes()
            .filter { it.syncState == SyncState.SYNC_FAILED.name }

        for (entity in failed) {
            // Reset to original pending state for retry
            val newState = when {
                entity.serverVersion == 0L -> SyncState.PENDING_CREATE
                else -> SyncState.PENDING_UPDATE
            }
            noteDao.updateSyncState(entity.id, newState.name)
        }
    }
}
