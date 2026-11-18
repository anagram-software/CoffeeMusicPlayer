# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Coffee Music Player: an Android local-music player (package `com.udeshcoffee.android`, published on the Play Store). Single `:app` module, mostly Kotlin with a few legacy Java files. minSdk 26, compileSdk 37, targetSdk 37.

A phased modernization is in progress (0 build system, 1 latest target SDK, 2 Media3, 3 library replacements, 4 Compose + Material 3, 5 hardening). Phases 0 (build system), 1 (target SDK 37, scoped storage, permissions) and 2 (Media3 playback) are done.

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

The only test is the placeholder `app/src/test/.../ExampleUnitTest.kt`. `android.enableJetifier=true` is still needed for SlidingUpPanel and RxBinding (support-library artifacts). The app theme uses `Theme.MaterialComponents.NoActionBar.Bridge`, which Material Components 1.14 requires.

**K2 gotcha:** a lambda whose last expression is a `when`/`if` containing `null` branches can now infer a nullable return type. With `Single.fromCallable` that crashes at runtime ("The callable returned a null value"). Use `Completable.fromAction` for side-effect-only work.

**LiveData gotcha:** since lifecycle 2.6 `Observer` is a Kotlin interface, so a `MutableLiveData<T>` (non-null `T`) that is set to `null` crashes its observer. Declare `MutableLiveData<T?>` when null is a valid value, and use `SingleLiveEvent<Unit>` + `call()` for data-less events (never `Void`).

**Platform rules (targetSdk 37):**
- Permissions are requested with the Activity Result API. Use `utils/Permissions` (`readAudio` is `READ_MEDIA_AUDIO` on 33+, `READ_EXTERNAL_STORAGE` below), never check `READ_EXTERNAL_STORAGE` directly.
- Modifying or deleting songs goes through `utils/MediaAccess` (`createWriteRequest` / `createDeleteRequest` consent dialogs on API 30+, launched with `StartIntentSenderForResult`). Files are never written by path; the tag editor copies to cache, edits with jaudiotagger, and writes back via `ContentResolver.openOutputStream(uri, "wt")`.
- Folders are derived from MediaStore paths (`MediaRepository.getChildFolders`), not by listing the file system. Storage roots come from `utils/Storage.getStorageRoots`.
- Activities are drawn edge to edge; View-based screens pad their root with `padForSystemBars()` / `padContentForSystemBars()` (`extensions/InsetsExtensions.kt`).
- Back handling uses `OnBackPressedCallback` (predictive back is enabled); call `onBackPressedDispatcher.onBackPressed()`, not `onBackPressed()`.
- PendingIntents are `FLAG_IMMUTABLE`.
- Home-screen widget buttons send media key events to Media3's `MediaButtonReceiver` (explicit, immutable PendingIntents with distinct request codes).

## Architecture

**DI (Koin 4).** `App.onCreate` calls `startKoin { androidContext(...); modules(appModule) }`. Fragments get ViewModels with `by viewModel()` (`org.koin.androidx.viewmodel.ext.android.viewModel`). Modules live in `di/`:
- `RepositoryModule`: Room database (single) and DAOs (`DataDao`, `PlaylistDao`), `RemoteDataSource`, `LocalDataSource`, `DataRepository`, `MediaRepository`
- `ViewModelModule`: every Koin-provided ViewModel. New ViewModels must be registered here.
- `CommonModule`: player fragments plus the two remaining MVP presenters

**Data layer (`data/`).** Two repositories with separate jobs:
- `MediaRepository` handles the device music library through `MediaStore` (songs, albums, artists, genres, folders) plus user playlists, which live in Room (`playlists` / `playlist_items`, holding MediaStore song IDs; MediaStore playlists are deprecated and were imported once on upgrade), using SQLBrite reactive queries on the ContentResolver. Albums and artists are built from song lists in `data/media/Operators.kt`.
- `DataRepository` handles app-owned data (lyrics, EQ presets, favorites, play stats, artist bios), combining `LocalDataSource` with `RemoteDataSource`.
- `local/LocalDatabase` is Room DB `CoffeePlayer.db` (Room 2.8, KSP), version 7, built with `allowMainThreadQueries()`. Schemas are exported to `app/schemas/` (commit them). A schema change needs a version bump and a `Migration` added to the `addMigrations(...)` call in `RepositoryModule`; copy the `createSql` from the exported JSON so the migration matches Room's validation.
- `remote/RemoteDataSource` is a Kotlin `object` that builds Retrofit clients on demand. It covers Genius (lyrics search, with the lyrics page scraped via Jsoup), Last.fm (artist info and images), and the `itunes/` package. Despite the package name, `itunes/` now calls the **Apple Music catalog API** (`api.music.apple.com/v1/catalog/us/`), which is used for tag and metadata lookup in the tag editor. API keys come from `BuildConfig` and go in as query params via an OkHttp interceptor. The Apple Music API sends no developer token, so tag lookup is broken; it is being replaced with Last.fm (authenticated).
- RxJava 2 is used throughout. Repository methods usually apply `subscribeOn(io)` / `observeOn(mainThread)` themselves.

**Playback (`playback/`, Media3).**
- `PlaybackService` is a `MediaLibraryService` owning an ExoPlayer. Media3 provides the notification, foreground state, media buttons (`androidx.media3.session.MediaButtonReceiver`), audio focus, becoming-noisy and playback resumption (`onPlaybackResumption`). The service adds queue persistence (`QueueStore`, same prefs keys/encoding as older releases), play counts, widget updates, the "on call" setting, audio effects (`AudioFXHelper` on a fixed audio session ID, exposed via `AudioEffects.helper`), voice search, and a browse tree for Android Auto (`LibraryBrowser`).
- The session exposes `SessionPlayer` (a `ForwardingSimpleBasePlayer`), not ExoPlayer directly. It implements fade on play/pause and the app's **visible shuffle**: shuffling reorders the queue itself (current song first) instead of ExoPlayer's hidden shuffle order, so queue indices in the UI are always play order. ExoPlayer's own `shuffleModeEnabled` stays false.
- `Song` <-> `MediaItem` conversion is in `playback/MediaItems.kt` (media ID = MediaStore audio ID). Controllers don't send playback URIs, so the session restores them in `onAddMediaItems` (`withPlaybackUri`).
- UI code talks to playback through `PlayerConnection` (Koin single wrapping a `MediaController`). `MainActivity`/`AppWidgetConfigureActivity` call `connect()`/`disconnect()`. Elsewhere use `getPlayer()`/`connectedPlayer()` (null until connected) and the helpers in `extensions/PlaybackExtensions.kt` (`playSong`, `queueSong`, `shuffle`).
- **`MediaController` (so every `PlayerConnection` method) must be called on the main thread**; calling it from an Rx background thread throws "MediaController method is called from a wrong thread". The playback helpers hop to main automatically.
- Change notifications: `playbackEvents()` (Rx stream of `PlaybackEvents` names: `METADATA_CHANGED`, `PLAYBACK_STATE_CHANGED`, `SERVICE_CONNECTED`). `METADATA_CHANGED` fires often (any timeline/state change), so check whether the song actually changed before doing expensive work. `ui/common/viewmodels/BasePlayerViewModel` subscribes for player-aware ViewModels, whose fragments must call `start()`/`stop()`.
- Repeat mode is stored in prefs as 1 = one, 2 = all, 3 = none (`RepeatModes` maps to `Player.REPEAT_MODE_*`).
- `LyricsCollectionWorker` (WorkManager, `dataSync` foreground) bulk-downloads missing lyrics.

**UI (`ui/`).**
- `MainActivity` hosts a Navigation Component graph (`res/navigation/nav_graph.xml`) for library, detail, search, editor, and equalizer screens. The player sits in a `SlidingUpPanelLayout` with `MiniPlayerFragment` and `PlayerFragment` (which contains the queue and lyrics).
- MVVM with LiveData is the current pattern. `EqualizerPresenter` and `LyricsPresenter` are leftover MVP code (`*Contract` + `BasePresenter`). Use ViewModels for new screens.
- Shared adapters, viewholders, dialogs, and base ViewModels (`SongContainingViewModel`, `DetailSong*ViewModel`) live in `ui/common/`.
- Images load through Glide 4. Album and artist art helpers are in `extensions/ArtExtensions.kt` and `utils/ArtUtils.kt`, and Palette-based theming is in `utils/PaletteUtils.kt`.

**Other.**
- Tag editing uses the bundled `app/libs/jaudiotagger-2.0.4-SNAPSHOT.jar`, with `TagOptionSingleton.isAndroid = true` set in `App`.
- Downloaded artist images are stored in app-private `filesDir/ArtistImages`.
- Home-screen widgets are in `widget/` (`BaseAppWidget` → `MediumAppWidget`, `BigAppWidget`).
