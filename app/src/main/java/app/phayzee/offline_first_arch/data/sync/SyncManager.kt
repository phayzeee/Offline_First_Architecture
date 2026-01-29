package app.phayzee.offline_first_arch.data.sync


import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.phayzee.offline_first_arch.data.repository.NoteRepository
import app.phayzee.offline_first_arch.domain.model.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sync state and coordinates between WorkManager and Repository.
 * Provides unified sync status observable for UI.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NoteRepository,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    init {
        observeWorkManager()
        observeNetworkAndPendingChanges()
    }

    /**
     * Monitor WorkManager state and update sync status.
     */
    private fun observeWorkManager() {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(SyncWorker.WORK_NAME)
            .observeForever { workInfos ->
                val workInfo = workInfos.firstOrNull()
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> _syncStatus.value = SyncStatus.SYNCING
                    WorkInfo.State.FAILED -> {
                        _syncStatus.value = SyncStatus.FAILED
                        _lastSyncError.value = "Sync failed. Tap to retry."
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _syncStatus.value = SyncStatus.IDLE
                        _lastSyncError.value = null
                    }
                    else -> { /* Keep current state */ }
                }
            }
    }

    /**
     * Update status based on network and pending changes.
     */
    private fun observeNetworkAndPendingChanges() {
        combine(
            networkMonitor.isOnline,
            repository.observePendingCount()
        ) { isOnline, pendingCount ->
            when {
                !isOnline -> SyncStatus.OFFLINE
                pendingCount > 0 && _syncStatus.value != SyncStatus.SYNCING -> SyncStatus.PENDING
                _syncStatus.value == SyncStatus.SYNCING -> SyncStatus.SYNCING
                else -> SyncStatus.IDLE
            }
        }.onEach { status ->
            if (_syncStatus.value != SyncStatus.SYNCING) {
                _syncStatus.value = status
            }
        }.launchIn(scope)
    }

    /**
     * Trigger immediate sync.
     */
    fun requestSync() {
        _lastSyncError.value = null
        SyncWorker.enqueueImmediate(context)
    }

    /**
     * Schedule sync when network is available.
     */
    fun scheduleSync() {
        SyncWorker.enqueue(context)
    }

    /**
     * Perform initial data load from remote.
     */
    suspend fun performInitialLoad(): Result<Unit> {
        return repository.initialLoad()
    }

    /**
     * Retry failed sync items.
     */
    fun retryFailed() {
        scope.launch {
            repository.retryFailedSync()
            requestSync()
        }
    }
}
