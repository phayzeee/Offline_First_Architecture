package app.phayzee.offline_first_arch.data.remote

import app.phayzee.offline_first_arch.domain.model.Note
import app.phayzee.offline_first_arch.domain.model.SyncState
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulates a remote API for notes.
 * In a real app, this would be a Retrofit service or similar.
 *
 * Implements realistic behavior:
 * - Network delays
 * - Occasional failures
 * - Server-side timestamps
 */
@Singleton
class FakeRemoteDataSource @Inject constructor() {

    // Simulated server-side storage
    private val serverNotes = mutableMapOf<String, RemoteNote>()

    // Control failure rate for testing (0.0 to 1.0)
    var failureRate: Float = 0.1f

    // Simulated network delay range in millis
    private val minDelay = 200L
    private val maxDelay = 800L

    /**
     * Fetch all notes from "server".
     */
    suspend fun fetchNotes(): Result<List<Note>> = simulateNetwork {
        serverNotes.values.map { it.toDomain() }
    }

    /**
     * Create a note on the "server".
     */
    suspend fun createNote(note: Note): Result<Note> = simulateNetwork {
        val remoteNote = RemoteNote(
            id = note.id,
            title = note.title,
            content = note.content,
            createdAt = note.createdAt.toEpochMilli(),
            updatedAt = System.currentTimeMillis(), // Server sets timestamp
            version = 1
        )
        serverNotes[note.id] = remoteNote
        remoteNote.toDomain()
    }

    /**
     * Update a note on the "server".
     */
    suspend fun updateNote(note: Note): Result<Note> = simulateNetwork {
        val existing = serverNotes[note.id]
            ?: throw NoSuchElementException("Note not found on server: ${note.id}")

        val updated = existing.copy(
            title = note.title,
            content = note.content,
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        serverNotes[note.id] = updated
        updated.toDomain()
    }

    /**
     * Delete a note from the "server".
     */
    suspend fun deleteNote(noteId: String): Result<Unit> = simulateNetwork {
        serverNotes.remove(noteId)
        Unit
    }

    /**
     * Get server version for conflict detection.
     */
    suspend fun getNoteVersion(noteId: String): Result<Long> = simulateNetwork {
        serverNotes[noteId]?.version ?: 0
    }

    /**
     * Simulates network call with delay and random failures.
     */
    private suspend fun <T> simulateNetwork(block: suspend () -> T): Result<T> {
        // Simulate network latency
        delay(Random.nextLong(minDelay, maxDelay))

        // Simulate random failures
        if (Random.nextFloat() < failureRate) {
            return Result.failure(NetworkException("Simulated network failure"))
        }

        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset server state (for testing).
     */
    fun reset() {
        serverNotes.clear()
    }

    /**
     * Seed server with initial data (for demo purposes).
     */
    fun seedData() {
        val sampleNotes = listOf(
            RemoteNote(
                id = "sample-1",
                title = "Welcome to Offline Notes",
                content = "This app demonstrates offline-first architecture. Try turning off your network and creating notes!",
                createdAt = System.currentTimeMillis() - 86400000,
                updatedAt = System.currentTimeMillis() - 86400000,
                version = 1
            ),
            RemoteNote(
                id = "sample-2",
                title = "How Sync Works",
                content = "Changes are stored locally first, then synced when network is available. The local database is always the source of truth.",
                createdAt = System.currentTimeMillis() - 43200000,
                updatedAt = System.currentTimeMillis() - 43200000,
                version = 1
            )
        )
        sampleNotes.forEach { serverNotes[it.id] = it }
    }
}

/**
 * Internal representation of server-side note.
 */
private data class RemoteNote(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        syncState = SyncState.SYNCED
    )
}

class NetworkException(message: String) : Exception(message)
