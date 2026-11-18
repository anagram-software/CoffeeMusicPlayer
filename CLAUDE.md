# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Coffee Music Player: an Android local-music player (package `com.udeshcoffee.android`, published on the Play Store). Single `:app` module, mostly Kotlin with a few legacy Java files. minSdk 26, compileSdk 37, targetSdk 37.

A phased modernization is in progress (0 build system, 1 latest target SDK, 2 Media3, 3 library replacements, 4 Compose + Material 3, 5 hardening). Phases 0 (build system), 1 (target SDK 37, scoped storage, permissions), 2 (Media3 playback), 3 (coroutines/Flow, Coil, Retrofit 3) and 4 (Compose + Material 3 UI) are done.

## Build & test

Gradle 9.9 wrapper, AGP 9.5 (built-in Kotlin, no `kotlin-android` plugin), Kotlin 2.4 (K2), KSP. Kotlin DSL build scripts, versions in `gradle/libs.versions.toml`. Builds on JDK 21 (bytecode target 17). Repositories are `google()` + `mavenCentral()` only.

```
./gradlew assembleDebug
./gradlew installDebug
./gradlew lintDebug                                                   # fails only on issues not in app/lint-baseline.xml
./gradlew test                                                        # JVM unit tests
./gradlew test --tests 'com.udeshcoffee.android.ExampleUnitTest'      # single test class
```

`local.properties` is untracked. Besides `sdk.dir` it holds API keys, which become `BuildConfig` fields (missing keys build as empty strings):
```
genius.accessToken=...
lastfm.apiKey=...
```

JVM unit tests live in `app/src/test` (e.g. `GeniusLyricsParserTest`). `MainActivity` uses the window theme `Theme.Coffee` (Material3 DayNight NoActionBar; real colours come from the Compose theme). `AppTheme` (`Theme.MaterialComponents.NoActionBar.Bridge`) remains only for the View-based widget configuration activity.

**K2 gotcha:** a lambda whose last expression is a `when`/`if` containing `null` branches can now infer a nullable return type. With `Single.fromCallable` that crashes at runtime ("The callable returned a null value"). Use `Completable.fromAction` for side-effect-only work.


**Platform rules (targetSdk 37):**
- Permissions are requested with the Activity Result API. Use `utils/Permissions` (`readAudio` is `READ_MEDIA_AUDIO` on 33+, `READ_EXTERNAL_STORAGE` below), never check `READ_EXTERNAL_STORAGE` directly.
- Modifying or deleting songs goes through `utils/MediaAccess` (`createWriteRequest` / `createDeleteRequest` consent dialogs on API 30+, launched with `StartIntentSenderForResult`). Files are never written by path; the tag editor copies to cache, edits with jaudiotagger, and writes back via `ContentResolver.openOutputStream(uri, "wt")`.
- Folders are derived from MediaStore paths (`MediaRepository.getChildFolders`), not by listing the file system. Storage roots come from `utils/Storage.getStorageRoots`.
- Activities are drawn edge to edge (`enableEdgeToEdge()`). Compose screens get insets from `Scaffold`/`safeDrawingPadding`; the View-based widget config screen uses `padForSystemBars()` (`extensions/InsetsExtensions.kt`).
- Back handling uses `OnBackPressedCallback` (predictive back is enabled); call `onBackPressedDispatcher.onBackPressed()`, not `onBackPressed()`.
- PendingIntents are `FLAG_IMMUTABLE`.
- Home-screen widget buttons send media key events to Media3's `MediaButtonReceiver` (explicit, immutable PendingIntents with distinct request codes).

## Architecture

**DI (Koin 4).** `App.onCreate` calls `startKoin { androidContext(...); modules(appModule) }`. Composables get ViewModels with `koinViewModel()` (parameterised ones with `koinViewModel(key = ...) { parametersOf(id) }`) and singletons with `koinInject()`. Modules live in `di/`:
- `RepositoryModule`: Room database (single) and DAOs (`DataDao`, `PlaylistDao`), `RemoteDataSource`, `LocalDataSource`, `DataRepository`, `MediaRepository`
- `ComposeModule`: every ViewModel. New ViewModels must be registered here.
- `CommonModule`: the default `SharedPreferences`

**Data layer (`data/`).** Two repositories with separate jobs:
- `MediaRepository` handles the device music library through `MediaStore` (songs, albums, artists, genres, folders) plus user playlists, which live in Room (`playlists` / `playlist_items`, holding MediaStore song IDs; MediaStore playlists are deprecated and were imported once on upgrade). `observe…` functions return Flows that re-query when MediaStore changes (a `ContentObserver` `callbackFlow`, combined with Room flows where relevant); `load…`/suspend functions are one-shot. Albums and artists are built from song lists in `data/media/Operators.kt`.
- `DataRepository` handles app-owned data (lyrics, EQ presets, favorites, play stats, artist bios), combining `LocalDataSource` with `RemoteDataSource`.
- `local/LocalDatabase` is Room DB `CoffeePlayer.db` (Room 2.8, KSP), version 7. DAOs are `suspend`/`Flow`; main-thread queries are not allowed. Schemas are exported to `app/schemas/` (commit them). A schema change needs a version bump and a `Migration` added to the `addMigrations(...)` call in `RepositoryModule`; copy the `createSql` from the exported JSON so the migration matches Room's validation.
- `remote/RemoteDataSource` is a Kotlin `object` (Retrofit 3 + OkHttp 5 + kotlinx.serialization, all `suspend`, returning null/empty on failure). It covers Genius (lyrics search; the lyrics page is fetched with OkHttp using browser headers — Genius blocks Jsoup's own client — and parsed by `extractLyrics`), and Last.fm (artist info, artist image scraped from the artist page, and `track.getInfo`/`track.search` for the tag editor's metadata lookup; Last.fm has no release year). API keys come from `BuildConfig` and go in as query params via an OkHttp interceptor.
- Coroutines/Flow throughout (no RxJava). Repository functions are main-safe. ViewModels expose `StateFlow` (repository flows via `stateIn(viewModelScope, ...)`), collected with `collectAsStateWithLifecycle()`; no LiveData. Fire-and-forget writes that must outlive a screen use `utils/ProcessLifecycleScope`.

**Playback (`playback/`, Media3).**
- `PlaybackService` is a `MediaLibraryService` owning an ExoPlayer. Media3 provides the notification, foreground state, media buttons (`androidx.media3.session.MediaButtonReceiver`), audio focus, becoming-noisy and playback resumption (`onPlaybackResumption`). The service adds queue persistence (`QueueStore`, same prefs keys/encoding as older releases), play counts, widget updates, the "on call" setting, audio effects (`AudioFXHelper` on a fixed audio session ID, exposed via `AudioEffects.helper`), voice search, and a browse tree for Android Auto (`LibraryBrowser`).
- The session exposes `SessionPlayer` (a `ForwardingSimpleBasePlayer`), not ExoPlayer directly. It implements fade on play/pause and the app's **visible shuffle**: shuffling reorders the queue itself (current song first) instead of ExoPlayer's hidden shuffle order, so queue indices in the UI are always play order. ExoPlayer's own `shuffleModeEnabled` stays false.
- `Song` <-> `MediaItem` conversion is in `playback/MediaItems.kt` (media ID = MediaStore audio ID). Controllers don't send playback URIs, so the session restores them in `onAddMediaItems` (`withPlaybackUri`).
- UI code talks to playback through `PlayerConnection` (Koin single wrapping a `MediaController`). `MainActivity`/`AppWidgetConfigureActivity` call `connect()`/`disconnect()`. Elsewhere use `getPlayer()`/`connectedPlayer()` (null until connected) and the helpers in `extensions/PlaybackExtensions.kt` (`playSong`, `queueSong`, `shuffle`).
- **`MediaController` (so every `PlayerConnection` method) must be called on the main thread**; calling it from a background thread throws "MediaController method is called from a wrong thread". The playback helpers hop to main automatically.
- Change notifications: `playbackEvents()` (a `SharedFlow` of `PlaybackEvents` names: `METADATA_CHANGED`, `PLAYBACK_STATE_CHANGED`, `SERVICE_CONNECTED`). `METADATA_CHANGED` fires often (any timeline/state change), so check whether the song actually changed before doing expensive work. For UI state prefer `PlayerConnection.state` (a `StateFlow<PlayerUiState>`: song, isPlaying, duration, queue, index, repeat, shuffle; composables use `rememberPlayerState()`) and `progress(intervalMs)` for the position.
- Repeat mode is stored in prefs as 1 = one, 2 = all, 3 = none (`RepeatModes` maps to `Player.REPEAT_MODE_*`).
- `LyricsCollectionWorker` (WorkManager, `dataSync` foreground) bulk-downloads missing lyrics.

**UI (`ui/`).**
- Jetpack Compose + Material 3, all under `ui/compose/`. `MainActivity` (a `ComponentActivity`) only handles permissions, the player connection, launch intents and delete consent, then calls `setContent { CoffeeApp(...) }`.
- `CoffeeApp`: `CoffeeTheme` (seeded from the current album art via materialkolor, else dynamic colour on 12+), a `NavigationSuiteScaffold` (Library, Favorites, Playlists, Equalizer, Settings; bar or rail by window size) around a navigation-compose `NavHost` with type-safe `@Serializable` routes (`navigation/Routes.kt`), the `MiniPlayer`, and the full `PlayerScreen` overlay (queue and lyrics inside; side-by-side when wider than tall).
- `AppActions` (provided as `LocalAppActions`) is the shared entry point for playback, navigation, option sheets (`OptionsSheetHost`), dialogs (`AppDialogHost`) and snackbar messages; screens call it rather than wiring those themselves.
- Reusable pieces are in `ui/compose/components/` (song rows, cards, artwork, sort menus backed by `SortSettings`/`SortManager` prefs, dialogs). Reorderable lists use `sh.calvin.reorderable`.
- Settings are a Compose screen over the same `SharedPreferences` keys (`utils/Config.kt`). Tag writing is in `utils/TagWriter.kt`.
- Images load through Coil 3 (singleton loader configured in `App`). Album art uses the `AlbumArt(albumId, version)` model and `AlbumArtFetcher` (MediaStore `loadThumbnail` on API 29+, where legacy `albumart` URIs don't resolve). Composables use `ui/compose/components/Artwork.kt`; bitmap/artist-image helpers are in `utils/ArtUtils.kt`. The theme seed (Palette) is computed in `ui/compose/theme/Theme.kt`.

**Other.**
- Tag editing uses jaudiotagger 3.0.1 (Maven Central), with `TagOptionSingleton.isAndroid = true` set in `App`. The editor writes only fields that differ from the song as it was when editing started.
- Downloaded artist images are stored in app-private `filesDir/ArtistImages`.
- Home-screen widgets are still RemoteViews in `widget/` (`BaseAppWidget` → `MediumAppWidget`, `BigAppWidget`), with a View-based `AppWidgetConfigureActivity` (the only AppCompat/XML-layout code left).
