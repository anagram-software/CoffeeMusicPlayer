# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Coffee Music Player: an Android local-music player (package `com.udeshcoffee.android`, published on the Play Store). Single `:app` module, mostly Kotlin with a few legacy Java files. minSdk 26, compileSdk 37, targetSdk 29 (raised in the modernization's Phase 1).

A phased modernization is in progress (0 build system, 1 latest target SDK, 2 Media3, 3 library replacements, 4 Compose + Material 3, 5 hardening). Phase 0 (build system) is done.

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

The only test is the placeholder `app/src/test/.../ExampleUnitTest.kt`. `android.enableJetifier=true` is still needed for Permiso, SlidingUpPanel and RxBinding (support-library artifacts). The app theme uses `Theme.MaterialComponents.NoActionBar.Bridge`, which Material Components 1.14 requires.

**K2 gotcha:** a lambda whose last expression is a `when`/`if` containing `null` branches can now infer a nullable return type. With `Single.fromCallable` that crashes at runtime ("The callable returned a null value"). Use `Completable.fromAction` for side-effect-only work.

## Architecture

**DI (Koin 4).** `App.onCreate` calls `startKoin { androidContext(...); modules(appModule) }`. Fragments get ViewModels with `by viewModel()` (`org.koin.androidx.viewmodel.ext.android.viewModel`). Modules live in `di/`:
- `RepositoryModule`: Room database (single) and DAO, `RemoteDataSource`, `LocalDataSource`, `DataRepository`, `MediaRepository`
- `ViewModelModule`: every Koin-provided ViewModel. New ViewModels must be registered here.
- `CommonModule`: player fragments plus the two remaining MVP presenters

**Data layer (`data/`).** Two repositories with separate jobs:
- `MediaRepository` handles the device music library through `MediaStore` (songs, albums, artists, genres, playlists, folders), using SQLBrite reactive queries on the ContentResolver. Albums and artists are built from song lists in `data/media/Operators.kt`.
- `DataRepository` handles app-owned data (lyrics, EQ presets, favorites, play stats, artist bios), combining `LocalDataSource` with `RemoteDataSource`.
- `local/LocalDatabase` is Room DB `CoffeePlayer.db` (Room 2.8, KSP), version 6, `exportSchema = false`, built with `allowMainThreadQueries()`. A schema change needs a version bump and a `Migration` added to the `addMigrations(...)` call in `RepositoryModule`.
- `remote/RemoteDataSource` is a Kotlin `object` that builds Retrofit clients on demand. It covers Genius (lyrics search, with the lyrics page scraped via Jsoup), Last.fm (artist info and images), and the `itunes/` package. Despite the package name, `itunes/` now calls the **Apple Music catalog API** (`api.music.apple.com/v1/catalog/us/`), which is used for tag and metadata lookup in the tag editor. API keys come from `BuildConfig` and go in as query params via an OkHttp interceptor. The Apple Music API sends no developer token, so tag lookup is broken; it is being replaced with Last.fm (authenticated).
- RxJava 2 is used throughout. Repository methods usually apply `subscribeOn(io)` / `observeOn(mainThread)` themselves.

**Playback (`service/`).**
- `MusicService` is the foreground service that owns the queue, MediaSession, notification (`NotifyManager`), audio effects (`AudioFXHelper`: EQ, bass boost, virtualizer), and widget updates.
- The only backend is `players/Player` (Android `MediaPlayer`) behind `players/BasePlayer`. The ExoPlayer 2.7 backend and the `pref_new_player` setting were removed in Phase 0, because ExoPlayer 2.7 was only published to jcenter. Phase 2 replaces all of this with Media3 (`MediaLibraryService` + ExoPlayer).
- UI code reaches the service through the static `ServiceConnectionUtil.binder`, using the `getService()` extensions in `extensions/Extentions.kt`. Start playback through the helpers in `extensions/PlaybackExtensions.kt` (`playSong`, `queueSong`, `shuffle`).
- The service tells the UI about changes with **LocalBroadcasts** (`MusicService.InternalIntents.METADATA_CHANGED`, `PLAYBACK_STATE_CHANGED`, `SERVICE_CONNECTED`). `ui/common/viewmodels/BasePlayerViewModel` subscribes to these via RxBroadcast. Player-aware ViewModels extend it, and their fragments must call `start()`/`stop()`.
- The queue and position are saved to SharedPreferences (`extensions/ServiceExtentions.kt`, with keys in `utils/Config.kt` → `PreferenceUtil`).
- `CollectionService` is a separate service that bulk-downloads lyrics and artist content in the background.

**UI (`ui/`).**
- `MainActivity` hosts a Navigation Component graph (`res/navigation/nav_graph.xml`) for library, detail, search, editor, and equalizer screens. The player sits in a `SlidingUpPanelLayout` with `MiniPlayerFragment` and `PlayerFragment` (which contains the queue and lyrics).
- MVVM with LiveData is the current pattern. `EqualizerPresenter` and `LyricsPresenter` are leftover MVP code (`*Contract` + `BasePresenter`). Use ViewModels for new screens.
- Shared adapters, viewholders, dialogs, and base ViewModels (`SongContainingViewModel`, `DetailSong*ViewModel`) live in `ui/common/`.
- Images load through Glide 4. Album and artist art helpers are in `extensions/ArtExtensions.kt` and `utils/ArtUtils.kt`, and Palette-based theming is in `utils/PaletteUtils.kt`.

**Other.**
- Tag editing uses the bundled `app/libs/jaudiotagger-2.0.4-SNAPSHOT.jar`, with `TagOptionSingleton.isAndroid = true` set in `App`.
- Writing to the SD card needs SAF permission handling (`utils/Storage.kt`, `PermissionToSdCardDialog`).
- Home-screen widgets are in `widget/` (`BaseAppWidget` → `MediumAppWidget`, `BigAppWidget`).
