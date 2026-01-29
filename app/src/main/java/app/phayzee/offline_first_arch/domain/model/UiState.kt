package app.phayzee.offline_first_arch.domain.model


/**
 * Represents the overall UI state for async operations.
 * Used consistently across the app for predictable state handling.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}

/**
 * Represents the current sync status shown to users.
 */
enum class SyncStatus {
    IDLE,       // No sync in progress, everything up to date
    SYNCING,    // Sync currently in progress
    PENDING,    // Changes waiting to be synced
    FAILED,     // Last sync attempt failed
    OFFLINE     // Device is offline
}

/**
 * Aggregated state for the notes list screen.
 */
data class NotesScreenState(
    val notes: UiState<List<Note>> = UiState.Loading,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val isOnline: Boolean = true,
    val pendingChangesCount: Int = 0
)

/**
 * State for note editor screen.
 */
data class NoteEditorState(
    val note: UiState<Note?> = UiState.Loading,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val error: String? = null
)
