# Offline Notes

A production-quality Android sample app demonstrating **offline-first architecture** using modern Android development tools.

## Overview

This app demonstrates how to build a robust Android application that works reliably offline, syncs data when network is available, and handles edge cases gracefully.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM + Clean Architecture |
| Async | Coroutines + Flow |
| Local Storage | Room |
| Background Sync | WorkManager |
| DI | Hilt |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Composable │  │  Composable │  │     Components      │ │
│  │   Screens   │  │  Navigation │  │  (Cards, Dialogs)   │ │
│  └──────┬──────┘  └─────────────┘  └─────────────────────┘ │
│         │                                                   │
│  ┌──────▼──────┐                                           │
│  │  ViewModels │  ← State hoisting, no business logic      │
│  └──────┬──────┘                                           │
└─────────┼───────────────────────────────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────────────┐
│                       Domain Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │    Note     │  │   UiState   │  │    SyncStatus       │ │
│  │   Model     │  │   Sealed    │  │      Enum           │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────────────┐
│                        Data Layer                           │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   Repository                         │   │
│  │  • Orchestrates local & remote sources              │   │
│  │  • Implements offline-first logic                   │   │
│  │  • Exposes Flows for reactive updates               │   │
│  └──────────────────────┬──────────────────────────────┘   │
│                         │                                   │
│         ┌───────────────┼───────────────┐                  │
│         ▼               ▼               ▼                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐  │
│  │    Room     │ │   Remote    │ │    SyncManager      │  │
│  │  Database   │ │ DataSource  │ │    + WorkManager    │  │
│  │  (Source    │ │   (Fake)    │ │                     │  │
│  │  of Truth)  │ │             │ │                     │  │
│  └─────────────┘ └─────────────┘ └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Offline-First Approach

### Core Principles

1. **Local database is the single source of truth**
   - All reads come from Room
   - UI observes local data via Flows
   - No direct dependency on network state for basic functionality

2. **Writes are always local first**
   - User actions immediately update local database
   - Changes are marked with a `SyncState` for later sync
   - UI reflects changes instantly (optimistic updates)

3. **Sync happens in the background**
   - WorkManager handles sync when conditions are met
   - Network connectivity triggers sync automatically
   - Manual pull-to-refresh available

### Data Flow

```
User Action → Local Database → UI Update (immediate)
                    ↓
              SyncManager → WorkManager → Remote API
                    ↓
              Update SyncState on success/failure
```

## Sync Strategy

### Sync States

Each note tracks its synchronization state:

| State | Description |
|-------|-------------|
| `SYNCED` | In sync with remote |
| `PENDING_CREATE` | Created locally, needs push |
| `PENDING_UPDATE` | Updated locally, needs push |
| `PENDING_DELETE` | Deleted locally, needs push |
| `SYNC_FAILED` | Last sync attempt failed |

### Sync Process

1. **Push Phase**: Upload local changes to remote
   - Process `PENDING_CREATE` → Create on server
   - Process `PENDING_UPDATE` → Update on server
   - Process `PENDING_DELETE` → Delete from server, then local

2. **Pull Phase**: Download remote changes
   - Fetch all notes from server
   - Update local database (skip items with pending changes)

### When Sync Runs

- App launch (initial data load)
- Network becomes available (via WorkManager constraints)
- Manual pull-to-refresh
- After local write operations (scheduled)

## UI State Modeling

All async operations use a sealed interface for predictable state handling:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

The UI observes a combined state that includes:
- Notes data (`UiState<List<Note>>`)
- Sync status (`SyncStatus`)
- Network connectivity (`Boolean`)
- Pending changes count (`Int`)

## Error Handling

| Scenario | Handling |
|----------|----------|
| Network failure | Show offline indicator, continue with local data |
| Sync failure | Mark items as `SYNC_FAILED`, show retry option |
| Empty state | Show friendly UI with create action |
| Note not found | Show error message with back navigation |
| Save failure | Show snackbar, keep user on screen |

## Key Design Decisions

### Why Room as Source of Truth?

- Provides reactive `Flow` APIs for automatic UI updates
- Survives process death and app restarts
- Handles complex queries efficiently
- Type-safe with compile-time verification

### Why WorkManager for Sync?

- Respects system resources and battery
- Survives app termination
- Built-in network constraints
- Automatic retry with exponential backoff
- Integrates well with Hilt

### Why Fake Remote Data Source?

- Demonstrates the architecture without external dependencies
- Simulates realistic network conditions (latency, failures)
- Easy to replace with real API (Retrofit) later
- Testable in isolation

## Trade-offs and Limitations

### Current Limitations

1. **No conflict resolution**: Last-write-wins strategy. In production, consider:
   - Server-side versioning
   - Three-way merge for text content
   - User-facing conflict resolution UI

2. **Full sync only**: No delta/incremental sync. For large datasets:
   - Implement timestamp-based sync
   - Use pagination for initial load
   - Consider GraphQL subscriptions

3. **Single user**: No authentication. For multi-user:
   - Add auth layer
   - Partition data by user
   - Handle token refresh in sync

4. **No offline conflict queue**: Concurrent offline edits on same note overwrite. Consider:
   - Operation-based sync (CRDT)
   - Conflict detection and resolution UI

### Future Improvements

- [ ] Conflict resolution UI
- [ ] Search functionality
- [ ] Note categories/tags
- [ ] Rich text editing
- [ ] Image attachments with offline caching
- [ ] Export/import functionality
- [ ] Widget support

## Project Structure

```
app/src/main/java/com/example/offlinenotes/
├── data/
│   ├── local/           # Room database, entities, DAOs
│   ├── remote/          # Fake remote data source
│   ├── repository/      # Repository implementation
│   └── sync/            # SyncManager, NetworkMonitor, SyncWorker
├── di/                  # Hilt modules
├── domain/
│   └── model/           # Domain models, UI states
└── ui/
    ├── components/      # Reusable Compose components
    ├── navigation/      # Navigation setup
    ├── screens/         # Screen composables and ViewModels
    └── theme/           # Material 3 theming
```
## Testing the Offline Behavior

1. **Normal operation**: Create, edit, delete notes - changes sync automatically
2. **Airplane mode**: 
   - Enable airplane mode
   - Make changes (create/edit/delete notes)
   - Notice the "Offline mode" indicator
   - Changes are saved locally and show pending status
3. **Reconnection**:
   - Disable airplane mode
   - Watch the sync happen automatically
   - Pending indicators clear as items sync

## License

MIT License - feel free to use this as a starting point for your own projects.

---

Built with ❤️ using modern Android development practices.
