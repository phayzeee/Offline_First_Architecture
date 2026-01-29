package app.phayzee.offline_first_arch.ui.screens


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.phayzee.offline_first_arch.data.repository.NoteRepository
import app.phayzee.offline_first_arch.data.sync.SyncManager
import app.phayzee.offline_first_arch.domain.model.Note
import app.phayzee.offline_first_arch.domain.model.NoteEditorState
import app.phayzee.offline_first_arch.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NoteRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]
    val isNewNote: Boolean = noteId == null

    private val _state = MutableStateFlow(NoteEditorState())
    val state: StateFlow<NoteEditorState> = _state.asStateFlow()

    // Form state - separate from UI state for better control
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private var currentNote: Note? = null

    init {
        if (!isNewNote && noteId != null) {
            loadNote(noteId)
        } else {
            _state.update { it.copy(note = UiState.Success(null)) }
        }
    }

    private fun loadNote(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(note = UiState.Loading) }
            try {
                val note = repository.getNoteById(id)
                if (note != null) {
                    currentNote = note
                    _title.value = note.title
                    _content.value = note.content
                    _state.update { it.copy(note = UiState.Success(note)) }
                } else {
                    _state.update { it.copy(note = UiState.Error("Note not found")) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(note = UiState.Error(e.message ?: "Failed to load note")) }
            }
        }
    }

    fun onTitleChange(value: String) {
        _title.value = value
    }

    fun onContentChange(value: String) {
        _content.value = value
    }

    fun saveNote() {
        val titleValue = _title.value.trim()
        val contentValue = _content.value.trim()

        if (titleValue.isBlank() && contentValue.isBlank()) {
            _state.update { it.copy(error = "Note cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                if (isNewNote) {
                    repository.createNote(
                        title = titleValue.ifBlank { "Untitled" },
                        content = contentValue
                    )
                } else {
                    currentNote?.let { note ->
                        repository.updateNote(
                            note = note,
                            title = titleValue.ifBlank { "Untitled" },
                            content = contentValue
                        )
                    }
                }

                // Schedule background sync
                syncManager.scheduleSync()

                _state.update {
                    it.copy(
                        isSaving = false,
                        savedSuccessfully = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save note"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun hasUnsavedChanges(): Boolean {
        val currentTitle = _title.value.trim()
        val currentContent = _content.value.trim()

        return if (isNewNote) {
            currentTitle.isNotBlank() || currentContent.isNotBlank()
        } else {
            currentNote?.let { note ->
                note.title != currentTitle || note.content != currentContent
            } ?: false
        }
    }
}
