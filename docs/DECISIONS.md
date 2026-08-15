# Engineering Decisions

This file records deviations from the HLD/LLD and toolchain choices made while
building SmartFiles. It is the source of truth for "why" when the code and the
specs disagree.

## Toolchain

| Item          | Choice                                   | Rationale                                                                                                                    |
| ------------- | ---------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| AGP           | 9.0.0                                    | Latest stable at scaffold time.                                                                                              |
| Kotlin        | 2.2.10 (provided by AGP 9)               | AGP 9 has built-in Kotlin. `org.jetbrains.kotlin.android` must NOT be applied.                                               |
| KSP           | 2.2.10-2.0.2                             | Room + Hilt annotation processing.                                                                                           |
| Hilt          | 2.60.1, `enableAggregatingTask = true`   | Aggregating task required by AGP 9.                                                                                          |
| Gradle        | 9.1.0                                    | Wrapper generated from the cached distribution on this machine.                                                              |
| Java          | 17 toolchain                             | JVM modules use `sourceCompatibility`/`targetCompatibility = 17`.                                                            |

### Workarounds for AGP 9 built-in Kotlin

1. `gradle.properties` sets `android.disallowKotlinSourceSets=false`
   (experimental). Without it, KSP-generated source sets cannot be registered
   for the built-in Kotlin support.
2. The `kotlin-jvm` catalog plugin alias is **version-less**. Pure JVM modules
   (`domain`, `core:common`, `core:model`) apply `org.jetbrains.kotlin.jvm`;
   AGP 9 puts KGP on the classpath with a version, so specifying a version
   fails with "plugin already on the classpath with an unknown version".
3. Compose modules apply `org.jetbrains.kotlin.plugin.compose` (version = KGP).

## Dependency versions

AGP 9.0.0 rejects AAR metadata compiled against a newer `compileSdk` (max
recommended is 36). The whole androidx stack was pinned to the API-36
generation so the build stays on `compileSdk = 36`:

- core-ktx 1.17.0, activity-compose 1.11.0
- lifecycle 2.10.0, navigation 2.9.6, work 2.10.3, room 2.8.2
- datastore 1.1.7, hilt-navigation-compose 1.2.0, compose BOM 2025.06.00

## Module layout

Clean architecture, one Gradle module per boundary (per LLD §2):

- `app` — application, MainActivity, DI root.
- `feature` — single UI module (home/albums/search) with `@HiltViewModel`s.
- `data` — repository implementations, Hilt modules (`DataModule`,
  `AppBindingsModule`), DataStore/FileSource/ML wiring.
- `domain` — use cases + repository interfaces. Depends only on
  `core:model` + `javax.inject` (annotations).
- `core:common`, `core:model`, `core:database`, `core:datastore`,
  `core:filesystem`, `core:ml`, `core:designsystem`.

## FTS / ranking

- Search indexes use FTS4 **external content** (`contentEntity`) so the
  virtual table cannot store extra per-row data and the hidden `rank` column is
  not exposed through Room. `SearchDao` therefore returns `List<FileEntity>`
  ordered by `rowid`; **true relevance ranking is deferred to Phase 3**
  (semantic + hybrid weighted scoring). Keyword search itself is fully
  functional in Phase 2.
- `FileDao.observeRecent` takes no `limit` parameter; SQL uses a fixed `LIMIT`.

## Storage / SAF

- The Room database is a **virtual index only**. Files are never moved,
  renamed, or rewritten (`core:ml` and `FileRepositoryImpl`).
- Re-scan preserves processing columns (`processingStatus`, `processingLevel`,
  `extractedText`, `primaryAlbumId`, `perceptualHash`) by merging over the
  existing row keyed by URI (`FileDao.getByUri`). Only change-detection
  metadata is refreshed.

## ML Kit / extraction

- OCR: `com.google.mlkit.vision.text.latin.TextRecognizerOptions`.
- Image labeling: `com.google.mlkit.vision.label.defaults.ImageLabelerOptions`.
- ML Kit task awaits via `kotlinx-coroutines-play-services`.
- AI/ML models are only reachable behind `domain.ContentExtractor` and related
  interfaces; no model code leaks into the UI.

## Build performance (host constraints)

- Host has ~7.3 GB RAM and a small page file; full parallel builds hang the
  machine. `gradle.properties`: sequential (`org.gradle.parallel=false`),
  capped workers (`workers.max=2`), no persistent daemon
  (`org.gradle.daemon=false`), `-Xmx2560m` Gradle / `-Xmx768m` Kotlin daemon.
- During development prefer `:app:compileDebugKotlin` (validates all Kotlin +
  Hilt/KSP codegen) over a full `assembleDebug`.

## Room FK indices

`androidx.room` emits warnings for FK child columns without indexes. Added
explicit `indices = [Index(...)]` to: `FileAlbumCrossRef` (albumId),
`FileTagCrossRef` (tagId), `DuplicateGroupMemberEntity` (fileId),
`EmbeddingEntity` (fileId).

## Manifest / permissions

Only `FOREGROUND_SERVICE` and `POST_NOTIFICATIONS` are declared. No storage
permissions: file access is SAF `OpenDocumentTree` only.
