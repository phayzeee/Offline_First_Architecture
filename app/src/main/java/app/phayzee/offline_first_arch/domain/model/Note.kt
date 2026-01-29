package app.phayzee.offline_first_arch.domain.model


import java.time.Instant

/**
 * Domain model representing a Note.
 * Contains both content and sync metadata for offline-first operations.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState = SyncState.SYNCED
) {
    companion object {
        fun create(title: String, content: String): Note {
            val now = Instant.now()
            return Note(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                content = content,
                createdAt = now,
                updatedAt = now,
                syncState = SyncState.PENDING_CREATE
            )
        }
    }

    fun update(title: String, content: String): Note = copy(
        title = title,
        content = content,
        updatedAt = Instant.now(),
        syncState = if (syncState == SyncState.PENDING_CREATE) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE
    )

    fun markForDeletion(): Note = copy(
        syncState = SyncState.PENDING_DELETE,
        updatedAt = Instant.now()
    )

    fun markSynced(): Note = copy(syncState = SyncState.SYNCED)
}

/**
 * Represents the synchronization state of a Note.
 */
enum class SyncState {
    SYNCED,          // In sync with remote
    PENDING_CREATE,  // Created locally, needs to be pushed
    PENDING_UPDATE,  // Updated locally, needs to be pushed
    PENDING_DELETE,  // Deleted locally, needs to be pushed
    SYNC_FAILED      // Last sync attempt failed
}
