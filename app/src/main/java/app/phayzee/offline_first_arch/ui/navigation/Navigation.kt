package app.phayzee.offline_first_arch.ui.navigation


import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.phayzee.offline_first_arch.ui.screens.NoteEditorScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val NOTES_LIST = "notes"
    const val NOTE_EDITOR = "note/{noteId}"
    const val NOTE_CREATE = "note/new"

    fun noteEditor(noteId: String) = "note/$noteId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.NOTES_LIST
    ) {
        composable(Routes.NOTES_LIST) {
            NotesListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(Routes.noteEditor(noteId))
                },
                onCreateNote = {
                    navController.navigate(Routes.NOTE_CREATE)
                }
            )
        }

        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType }
            )
        ) {
            NoteEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.NOTE_CREATE) {
            NoteEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
