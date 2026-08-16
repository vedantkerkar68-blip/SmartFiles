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
- **Scanned-PDF OCR renders pages with pdfbox-android, not the platform
  `android.graphics.PdfRenderer`.** The platform class moved to
  `android.graphics.pdf` in API 35/36 and is unavailable on the lower API
  levels SmartFiles supports, while pdfbox-android's
  `PDFRenderer.renderImageWithDPI` returns an `android.graphics.Bitmap` on all
  supported API levels. Textless/tiny-text PDFs fall back to per-page OCR via
  `ScannedPdfOcrExtractor` (stage the PDF to `context.cacheDir`, render at up to
  ~160 DPI capped at 2500 px, `MlKitEngine.recognise` per page, cap 30 pages;
  staged file always deleted in `finally`). Extraction source and average OCR
  confidence are recorded on the result.

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

## Background work (Phase 1)

- New `core:workmanager` module holds `WorkNames` + `WorkConstraints` presets so
  the data layer does not hardcode strings/builder flags.
- WorkManager is configured via `SmartFilesApp : Configuration.Provider`
  (`HiltWorkerFactory`). The default `androidx.startup.InitializationProvider`
  is suppressed in the manifest so `@HiltWorker` classes get Hilt dependencies.
- Workers live in `:data` (`data/worker`). The UI-facing contract is the domain
  interface `BackgroundWorkScheduler`; `feature` never imports WorkManager.
- `WorkManager` unique-work naming decision: the one-time immediate metadata
  scan uses a **separate** name (`METADATA_SCAN_ONCE`, `REPLACE`) from the
  periodic scan (`METADATA_SCAN`, `KEEP`). Reusing one name with `REPLACE`
  would silently cancel the periodic work after the first user-triggered scan.
- `DeepProcessingWorker` drains the queue in loops and commits each file's
  extraction atomically (mark IN_PROGRESS → extract → update Room → mark DONE;
  failures get exponential backoff via `nextEligibleAt`). It is re-triggered by
  `ProcessingQueueRepositoryImpl.enqueue` and periodic scans, not by
  self-rescheduling (which is a no-op under `UniqueWork.KEEP` while running).
- `UserTriggeredProcessingWorker` ("process now") runs with no constraints as
  normal unique work. A foreground-service upgrade (deep-progress notification)
  is a later enhancement to avoid API-34 `foregroundServiceType` complexity.
- `MetadataScanWorker` walks every ACTIVE SAF tree plus MediaStore, syncs via
  `FileRepository.syncScan`, and enqueues changed files for deep processing.

## Storage / permission lifecycle (Phase 1)

- Folder grants are now a **real Room table**: `indexed_folders`,
  schema-exported
  (`room.schemaLocation=$projectDir/schemas`, `exportSchema=true`); committed
  JSON under `core/database/schemas/` is the migration source of truth.
- `FolderPermissionState { ACTIVE, NEEDS_REGRANT, REVOKED }`.
- On grant loss the folder is flagged `NEEDS_REGRANT`; **the index is never
  deleted** (spec §40). Re-granting restores it; removal deletes only the grant
  row, not indexed files.
- `takePersistableUriPermission` is best-effort: providers that cannot persist
  a grant throw `SecurityException`, which is caught — the folder still indexes
  for the session and `PermissionValidationWorker` flags it later if it lapses.
- `MediaStoreFileDiscoverySource` adds Images + Downloads (API 29+) with no
  tree grant; results de-dupe by URI. MediaStore `DATE_MODIFIED` is seconds →
  multiplied to millis to stay consistent with the rest of the app.
- FTS content entity `files_fts` and the new `indexed_folders` table coexist in
  the version-1 schema; no migration needed until the schema changes.

## Classification & albums (Phase 3)

- **Confidence scoring renormalizes over available evidence.** The LLD §4.3a
  formula (`0.45·keyword + 0.35·centroid + 0.15·cluster + 0.05·history`) is kept
  exactly, but components whose data source does not exist yet (album-centroid
  cosine until Phase 4, correction history until Phase 6) contribute their
  weight to *neither* numerator nor denominator (`ConfidenceScorer`). The
  returned score stays on the 0..1 scale and `Verdict.coverage` reports what
  fraction of the evidence was real, so a high score is never fabricated from
  missing signals. With only keyword + cluster agreement present (Phase 3),
  coverage is 0.60 and strong keyword matches can still clear the 85% gate.
- **Cluster agreement is computed from term-profile overlap, not embeddings.**
  `existingClusterAgreement` = fraction of up to 40 recently-classified files
  whose bag-of-words profile is cosine-similar (≥0.4) to the target *and* whose
  album matches the predicted top-level album. Real, bounded, corpus-relative.
- **Dynamic sub-album creation is gated on distinctness evidence.** Per LLD
  §4.3b, AUTO_CREATE requires `size ≥ 5 ∧ cohesion ≥ 0.78 ∧ distinctness ≥ 0.35`.
  Distinctness is `1 − max cosine to existing album centroids`, which does not
  exist before Phase 4 embeddings, so AUTO_CREATE is honestly held back; the
  suggest branch (`size ≥ 3 ∧ cohesion ≥ 0.65`) runs today over term-profile
  cohesion and surfaces draft `AUTO_CREATED` sub-albums plus per-file
  suggestions. Greedy single-pass clustering over a bounded candidate set
  (400 files, min similarity 0.5), throttled to once per 10 minutes.
- **Structured spans use local regex, not ML Kit Entity Extraction.** The LLD
  names the ML Kit Entity Extraction API, which downloads models at runtime —
  against the local-first/no-network stance. `DateSpanExtractor` emits
  `date:YYYY-MM-DD` / `year:YYYY` metadata tags instead; addresses are deferred
  with the entity API until a user opts into network-backed processing.
- **Tags are persisted and feed FTS.** `TagRepositoryImpl` replaces a file's
  tag set (dedup by name) and refreshes the denormalized `files.tagsConcat`, so
  keyword search over tags works once the FTS row updates (Phase 4).
- **Photos is media-type-driven.** Images whose extraction yields no text are
  assigned to the seeded Photos album at 0.90 (media-type evidence) unless a
  content category scores higher; photos stay searchable either way.
- **Schema bumped to v2** to add the `album_suggestions` table (persisted
  suggestions survive process death and back the Albums UI accept/reject flow).
  `fallbackToDestructiveMigration` is still active pre-release; real migration
  tests remain a Phase-7 hardening item.
- **`AlbumSuggestion` carries `fileName`** for display; rejected (file, album)
  pairings are remembered so the same pairing is never re-suggested, mirroring
  the "not a duplicate" correction pattern.
- **Queue targetLevel now means what it says.** `TARGET_LEVEL_ALL = 3`;
  `DeepProcessingWorker` advances a file to Level-2 (extraction), then Level-3
  (classification + tags + assign/suggest) for items enqueued to Level-3.
