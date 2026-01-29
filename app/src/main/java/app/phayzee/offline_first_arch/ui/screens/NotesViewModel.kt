package app.phayzee.offline_first_arch.ui.screens


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.phayzee.offline_first_arch.data.repository.NoteRepository
import app.phayzee.offline_first_arch.data.sync.NetworkMonitor
import app.phayzee.offline_first_arch.data.sync.SyncManager
import app.phayzee.offline_first_arch.domain.model.Note
import app.phayzee.offline_first_arch.domain.model.NotesScreenState
import app.phayzee.offline_first_arch.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val screenState: StateFlow<NotesScreenState> = combine(
        repository.observeNotes()
            .catch { emit(emptyList()) },
        syncManager.syncStatus,
        networkMonitor.isOnline,
        repository.observePendingCount(),
        _isLoading
    ) { notes, syncStatus, isOnline, pendingCount, isLoading ->
        NotesScreenState(
            notes = when {
                isLoading -> UiState.Loading
                else -> UiState.Success(notes)
            },
            syncStatus = syncStatus,
            isOnline = isOnline,
            pendingChangesCount = pendingCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesScreenState()
    )

    init {
        performInitialLoad()
    }

    private fun performInitialLoad() {
        viewModelScope.launch {
            _isLoading.value = true
            syncManager.performInitialLoad()
            _isLoading.value = false
            // Schedule sync for any pending changes
            syncManager.scheduleSync()
        }
    }

    fun onRefresh() {
        syncManager.requestSync()
    }

    fun onRetrySync() {
        syncManager.retryFailed()
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            syncManager.scheduleSync()
        }
    }
}
