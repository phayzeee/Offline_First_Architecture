package app.phayzee.offline_first_arch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.phayzee.offline_first_arch.domain.model.Note
import app.phayzee.offline_first_arch.domain.model.SyncState
import java.time.Instant

/**
 * Room entity representing a Note in the local database.
 * Maps directly to the notes table.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,      // Stored as epoch millis
    val updatedAt: Long,
    val syncState: String,    // Stored as enum name
    val serverVersion: Long = 0  // For conflict detection
) {
    fun toDomain(): Note = Note(
        id = id,
        title = title,
        content = content,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        syncState = SyncState.valueOf(syncState)
    )

    companion object {
        fun fromDomain(note: Note, serverVersion: Long = 0): NoteEntity = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            createdAt = note.createdAt.toEpochMilli(),
            updatedAt = note.updatedAt.toEpochMilli(),
            syncState = note.syncState.name,
            serverVersion = serverVersion
        )
    }
}