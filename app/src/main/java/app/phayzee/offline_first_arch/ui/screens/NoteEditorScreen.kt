package app.phayzee.offline_first_arch.ui.screens


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.phayzee.offline_first_arch.domain.model.UiState
import app.phayzee.offline_first_arch.ui.components.ErrorState
import app.phayzee.offline_first_arch.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Handle successful save
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            onNavigateBack()
        }
    }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Handle back with unsaved changes
    val handleBack = {
        if (viewModel.hasUnsavedChanges()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler { handleBack() }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            onConfirm = onNavigateBack,
            onDismiss = { showDiscardDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(if (viewModel.isNewNote) "New Note" else "Edit Note")
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = viewModel::saveNote,
                            enabled = !state.isSaving
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                )

                if (state.isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val noteState = state.note) {
            is UiState.Loading -> LoadingState(Modifier.padding(padding))

            is UiState.Error -> ErrorState(
                message = noteState.message,
                onRetry = { /* Retry not applicable here */ },
                modifier = Modifier.padding(padding)
            )

            is UiState.Success -> {
                NoteEditorContent(
                    title = title,
                    content = content,
                    onTitleChange = viewModel::onTitleChange,
                    onContentChange = viewModel::onContentChange,
                    isNewNote = viewModel.isNewNote,
                    modifier = Modifier
                        .padding(padding)
                        .imePadding()
                )
            }
        }
    }
}

@Composable
private fun NoteEditorContent(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    isNewNote: Boolean,
    modifier: Modifier = Modifier
) {
    val titleFocusRequester = remember { FocusRequester() }

    // Auto-focus title field for new notes
    LaunchedEffect(isNewNote) {
        if (isNewNote) {
            titleFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Title field
        TextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = {
                Text(
                    "Title",
                    style = TextStyle(fontSize = 24.sp)
                )
            },
            textStyle = TextStyle(fontSize = 24.sp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocusRequester),
            colors = transparentTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        // Content field
        TextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text("Start writing...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = transparentTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences
            )
        )
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard changes?") },
        text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep editing")
            }
        }
    )
}
