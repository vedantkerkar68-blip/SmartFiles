# Implementation Status

Current phase tracking for SmartFiles (spec §33). Source of truth for what is
built, what is missing, and known deviations. Companion: `docs/DECISIONS.md`.

## Overall verdict

**NOT a complete/full running app.** Phase 0 (build foundation) and Phase 1
(storage + database) are complete; Phase 2 (content extraction, driven by the
deep-processing worker) is functional but still missing the scanned-PDF OCR
fallback; Phases 3–7 are not. The repo is a compile-clean app with a working
vertical slice: pick a SAF folder → persistence + background metadata scan →
MediaStore auto-discovery → Level-1 metadata lands in Room → Level-2 extraction
is drained from a persistent queue → counts/index show on Home.

## Phase status

| Phase | Goal | Status | Notes |
|---|---|---|---|
| 0 — Build foundation | Clean build | **COMPLETE** | `assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (≈130 MB debug). Hilt/KSP/Compose/navigation all wired. |
| 1 — Storage + database | Folder → real files in DB | **COMPLETE** | SAF walk + Room entities/DAOs + syncScan + queue tables + persistable-permission bookkeeping (`indexed_folders`, `FolderPermissionState`, `PermissionValidationWorker`) + MediaStore auto-discovery + `core:workmanager` with `MetadataScanWorker`/`DeepProcessingWorker`/`UserTriggeredProcessingWorker` + schema export (`core/database/schemas`). Still uses `fallbackToDestructiveMigration` — real migration tests are a Phase-7 hardening item. |
| 2 — Content extraction | Content becomes searchable | **PARTIAL** | PdfBox text layer + ML Kit image OCR/labels + perceptual hash + thumbnail code exists. `DeepProcessingWorker` now drives the pipeline (queue → `ContentExtractor` → `FileDao.updateContent`), and `syncScan` re-enqueues changed files. Missing: scanned-PDF OCR fallback (page render + ML Kit) and batch/caching refinements. |
| 3 — Classification + albums | Auto-organized virtual albums | **NOT STARTED** | Only interfaces: `ClassificationEngine`, `ClassificationStrategy`, `AlbumDecision`, `AlbumRepository`. No impl, no lexicon, no scorer, no album creator, no suggestions UI. |
| 4 — Embeddings + semantic search | Semantic retrieval | **NOT STARTED** | Only `EmbeddingCodec` + `EmbeddingDao` + `EmbeddingRepository` interface. No LiteRT `EmbeddingModelManager`, generator, representative-text builder, or chunked vector search. No model file bundled. |
| 5 — Hybrid search | Fused explainable ranking | **NOT STARTED** | FTS `SearchDao` + `SearchRepository` interface + `QueryIntentParser` interface only. No orchestrator, score fusion, or ranking. No Search UI. |
| 6 — Intelligence | Duplicates / related / corrections | **NOT STARTED** | Interfaces only. No `DuplicateDetectionRepositoryImpl`, finders, `RelatedFilesResolver`, `CorrectionProcessor`, or `UserCorrectionRepositoryImpl`. |
| 7 — Hardening | Profiling, tests, device validation | **NOT STARTED** | No tests of any kind; no lint config; no perf/memory measurements; no device testing. |

## Complete / working now

- 11-module Gradle build (AGP 9.0.0, Kotlin 2.2.10, KSP, Hilt 2.60.1).
- All 11 Room entities (incl. `indexed_folders`) + FTS4 external-content table, DAOs, type converters; schema JSON exported and committed under `core/database/schemas/`.
- `SafFileSource` tree walk (hidden-dir + `.thumbnails` exclusion, depth guard).
- `MediaStoreFileDiscoverySource` (Images + Downloads on API 29+, no tree grant, de-duped by URI) behind `MediaFileDiscoverySource`.
- `MimeTypeMapper` shared by SAF + MediaStore sources.
- `FileChangeDetector` (size/mtime only; SHA-256 is not the change signal).
- `FileRepositoryImpl.syncScan` transaction; preserves processing columns on re-scan; marks missing URIs deleted; `updateProcessingResult(fileId, ExtractionResult)` persists Level-2 extraction into Room.
- `SettingsRepositoryImpl` (Preferences DataStore, all 23 tunables from LLD §13).
- `ProcessingQueueRepositoryImpl` + `QueueDao` (persistent queue, priority/retry/backoff fields); enqueue auto-triggers the deep-processing worker.
- `FolderRepositoryImpl` + `FolderDao` + `FolderPermissionState` (persist SAF grants, `takePersistableUriPermission`, `refreshPermissionStates`, release on remove; grant loss flags `NEEDS_REGRANT` without deleting the index).
- `core:workmanager` module (`WorkNames`, `WorkConstraints`).
- `MetadataScanWorker` (periodic 4 h + one-time immediate via `BackgroundWorkScheduler`), `DeepProcessingWorker`, `UserTriggeredProcessingWorker`, `PermissionValidationWorker` (daily) — all `@HiltWorker`.
- `SmartFilesApp : Application(), Configuration.Provider` with `HiltWorkerFactory`; default `androidx.startup` initializer suppressed in the manifest.
- `EmbeddingCodec` (IEEE-754 half-float pack/unpack, dot, normalize).
- `ContentExtractorImpl` (PDF text layer via PdfBox; image OCR/labels/thumbnail/perceptual hash via ML Kit).
- `PerceptualHasher` (64-bit aHash), `ImageBitmapDecoder` (sampled decode).
- `HomeViewModel` + `HomeScreen` (SAF `OpenDocumentTree` → persist grant → background scan; count; re-scan).
- `docs/DECISIONS.md`.

## Missing — required for "full running app" (spec §40)

- Onboarding, Search, Album browser, File detail, Related files, Duplicates review, Settings, Processing-status, Suggestions/corrections, Permission-state UI.
- Room **migration tests** (schema JSON exists; `AppDatabase` is still v1 with `fallbackToDestructiveMigration` active — replace with `AutoMigration` + migration tests before release).
- `AlbumRepositoryImpl`, `SearchRepositoryImpl`, `EmbeddingRepositoryImpl`, `DuplicateDetectionRepositoryImpl`, `UserCorrectionRepositoryImpl`.
- Classification engine: `CategoryLexicon`, `ConfidenceScorer`, `TagExtractor`, `DynamicAlbumCreator`, seed taxonomy.
- Embedding model: bundled quantized `.tflite` (target <30 MB), `EmbeddingModelManager` lifecycle, `RepresentativeTextBuilder`, `ChunkedVectorSearch`.
- Hybrid search: `HybridSearchOrchestrator`, `ScoreFusion`, real `QueryIntentParser`, explanation signals.
- Duplicates: exact (SHA-256), perceptual, semantic-version finders + review flow + "not a duplicate" memory.
- Related files engine + corrections/personalization (`CorrectionProcessor`).
- Scanned-PDF OCR fallback (page render + ML Kit).
- Tests (unit/Room/worker/storage/ML), lint, formatting, macrobenchmarks.
- `docs/IMPLEMENTATION_STATUS.md` now exists (this file).

## Known issues

- `fallbackToDestructiveMigration(dropAllTables = true)` is active (safe only pre-release; replace with migrations + migration tests in Phase 7).
- FTS4 external-content table does not expose `rank` to Room → keyword ordering is rowid-based until Phase 5 ranking lands (documented in `docs/DECISIONS.md`).
- `SafFileSource.getDocumentFileUri` returns `null` (photo-picker write-back path not needed in V1).
- Debug APK is ≈130 MB (Compose + ML Kit + PdfBox); release/R8 will shrink; target <150 MB.
- Host is RAM-constrained: builds must stay sequential with capped workers (see `docs/DECISIONS.md`).
- Background workers depend on device Doze/Foreground-service behavior; deep processing is opportunistic (battery-aware) rather than immediate for very large queues.

## Recommended next steps

1. ~~Commit the Phase-0 baseline~~ (done: `faece1d` "first commit", pushed to `origin/main`).
2. Commit Phase 1 (this tree) once verified.
3. Complete Phase 2: scanned-PDF OCR fallback (page render + ML Kit), then wire `batchId`/caching refinements if needed.
4. Then Phases 3–6 (classification/albums, embeddings, hybrid search, intelligence), each with its UI + tests.
5. Phase 7 hardening: migrations + migration tests, unit/Room/worker/ML tests, lint, profiling on the 4 GB reference device.
