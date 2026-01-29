package app.phayzee.offline_first_arch.ui.components


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.phayzee.offline_first_arch.domain.model.Note
import app.phayzee.offline_first_arch.domain.model.SyncState
import app.phayzee.offline_first_arch.domain.model.SyncStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Sync status banner shown at top of screen.
 */
@Composable
fun SyncStatusBar(
    syncStatus: SyncStatus,
    pendingCount: Int,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (syncStatus) {
            SyncStatus.IDLE -> MaterialTheme.colorScheme.primaryContainer
            SyncStatus.SYNCING -> MaterialTheme.colorScheme.secondaryContainer
            SyncStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
            SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
            SyncStatus.OFFLINE -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "syncBarColor"
    )

    val contentColor = when (syncStatus) {
        SyncStatus.IDLE -> MaterialTheme.colorScheme.onPrimaryContainer
        SyncStatus.SYNCING -> MaterialTheme.colorScheme.onSecondaryContainer
        SyncStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
        SyncStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        SyncStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AnimatedVisibility(
        visible = syncStatus != SyncStatus.IDLE,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = backgroundColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (syncStatus) {
                        SyncStatus.IDLE -> Icons.Default.CloudDone
                        SyncStatus.SYNCING -> Icons.Default.CloudSync
                        SyncStatus.PENDING -> Icons.Default.CloudQueue
                        SyncStatus.FAILED -> Icons.Default.Error
                        SyncStatus.OFFLINE -> Icons.Default.CloudOff
                    },
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = when (syncStatus) {
                        SyncStatus.IDLE -> "All synced"
                        SyncStatus.SYNCING -> "Syncing..."
                        SyncStatus.PENDING -> "$pendingCount change${if (pendingCount > 1) "s" else ""} pending"
                        SyncStatus.FAILED -> "Sync failed"
                        SyncStatus.OFFLINE -> "Offline mode"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                if (syncStatus == SyncStatus.FAILED) {
                    IconButton(onClick = onRetryClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry sync",
                            tint = contentColor
                        )
                    }
                }

                if (syncStatus == SyncStatus.SYNCING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Card displaying a note preview.
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (note.syncState == SyncState.PENDING_DELETE) 0.5f else 1f,
        label = "noteCardAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (note.syncState != SyncState.SYNCED) {
                    Spacer(modifier = Modifier.width(8.dp))
                    SyncIndicator(syncState = note.syncState)
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(note.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Small indicator showing sync state of individual note.
 */
@Composable
fun SyncIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (syncState == SyncState.PENDING_CREATE || syncState == SyncState.PENDING_UPDATE) 360f else 0f,
        animationSpec = tween(1000),
        label = "syncRotation"
    )

    val color = when (syncState) {
        SyncState.SYNCED -> MaterialTheme.colorScheme.primary
        SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> MaterialTheme.colorScheme.tertiary
        SyncState.PENDING_DELETE -> MaterialTheme.colorScheme.error
        SyncState.SYNC_FAILED -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .graphicsLayer { rotationZ = rotation }
            .background(color, RoundedCornerShape(4.dp))
    )
}

/**
 * Loading state placeholder.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state with retry button.
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Empty state when no notes exist.
 */
@Composable
fun EmptyState(
    onCreateNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No notes yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your first note to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onCreateNote) {
            Icon(Icons.Default.NoteAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create Note")
        }
    }
}

private fun formatDate(instant: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
