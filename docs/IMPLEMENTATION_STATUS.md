# Implementation Status

Current phase tracking for SmartFiles (spec §33). Source of truth for what is
built, what is missing, and known deviations. Companion: `docs/DECISIONS.md`.

## Overall verdict

**NOT a complete/full running app.** Phase 0 (build foundation) is complete;
Phases 1–7 are not. The repo is a compile-clean scaffold with a working
vertical slice: pick a SAF folder → metadata lands in Room → count shows on
Home. Everything beyond Level-1 metadata + content-extraction code is either an
interface, a stub, or absent.

## Phase status

| Phase | Goal | Status | Notes |
|---|---|---|---|
| 0 — Build foundation | Clean build | **COMPLETE** | `assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk` (124 MB debug). Hilt/KSP/Compose/navigation all wired. |
| 1 — Storage + database | Folder → real files in DB | **PARTIAL** | SAF walk + Room entities/DAOs + syncScan + queue tables done. Missing: MediaStore source, real migrations, WorkManager worker to drain the queue, permission-revocation handling, persistable-permission bookkeeping. |
| 2 — Content extraction | Content becomes searchable | **PARTIAL** | PdfBox text layer + ML Kit image OCR/labels + perceptual hash + thumbnail code exists. Missing: scanned-PDF OCR fallback, `DeepProcessingWorker` to drive the pipeline, extraction-result caching. |
| 3 — Classification + albums | Auto-organized virtual albums | **NOT STARTED** | Only interfaces: `ClassificationEngine`, `ClassificationStrategy`, `AlbumDecision`, `AlbumRepository`. No impl, no lexicon, no scorer, no album creator, no suggestions UI. |
| 4 — Embeddings + semantic search | Semantic retrieval | **NOT STARTED** | Only `EmbeddingCodec` + `EmbeddingDao` + `EmbeddingRepository` interface. No LiteRT `EmbeddingModelManager`, generator, representative-text builder, or chunked vector search. No model file bundled. |
| 5 — Hybrid search | Fused explainable ranking | **NOT STARTED** | FTS `SearchDao` + `SearchRepository` interface + `QueryIntentParser` interface only. No orchestrator, score fusion, or ranking. No Search UI. |
| 6 — Intelligence | Duplicates / related / corrections | **NOT STARTED** | Interfaces only. No `DuplicateDetectionRepositoryImpl`, finders, `RelatedFilesResolver`, `CorrectionProcessor`, or `UserCorrectionRepositoryImpl`. |
| 7 — Hardening | Profiling, tests, device validation | **NOT STARTED** | No tests of any kind; no lint config; no perf/memory measurements; no device testing. |

## Complete / working now

- 11-module Gradle build (AGP 9.0.0, Kotlin 2.2.10, KSP, Hilt 2.60.1).
- All 10 Room entities + FTS4 external-content table, DAOs, type converters.
- `SafFileSource` tree walk (hidden-dir + `.thumbnails` exclusion, depth guard).
- `FileChangeDetector` (size/mtime only; SHA-256 is not the change signal).
- `FileRepositoryImpl.syncScan` transaction; preserves processing columns on re-scan; marks missing URIs deleted.
- `SettingsRepositoryImpl` (Preferences DataStore, all 23 tunables from LLD §13).
- `ProcessingQueueRepositoryImpl` + `QueueDao` (persistent queue, priority/retry/backoff fields).
- `EmbeddingCodec` (IEEE-754 half-float pack/unpack, dot, normalize).
- `ContentExtractorImpl` (PDF text layer via PdfBox; image OCR/labels/thumbnail/perceptual hash via ML Kit).
- `PerceptualHasher` (64-bit aHash), `ImageBitmapDecoder` (sampled decode).
- `HomeViewModel` + `HomeScreen` (SAF `OpenDocumentTree`, scan, count, re-scan).
- `docs/DECISIONS.md`.

## Missing — required for "full running app" (spec §40)

- Onboarding, Search, Album browser, File detail, Related files, Duplicates review, Settings, Processing-status, Suggestions/corrections, Permission-state UI.
- WorkManager workers: `MetadataScanWorker`, `DeepProcessingWorker`, `UserTriggeredProcessingWorker`, `DuplicateScanWorker`, `PermissionValidationWorker` (spec §4.10). No `core:workmanager` module.
- MediaStore discovery source (LLD §4.1, §4.11).
- Room migrations (`AppDatabase` v1, `exportSchema=false`, destructive-fallback currently enabled; master prompt forbids blind destructive migration — must be replaced with real migrations before release).
- Persistable URI permission management + revoked-permission "needs re-grant" state.
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

- `fallbackToDestructiveMigration(dropAllTables = true)` is active (safe only pre-release; replace with migrations).
- FTS4 external-content table does not expose `rank` to Room → keyword ordering is rowid-based until Phase 5 ranking lands (documented in `docs/DECISIONS.md`).
- `SafFileSource.getDocumentFileUri` returns `null` (photo-picker write-back path not needed in V1).
- Debug APK is 124 MB (Compose + ML Kit + PdfBox); release/R8 will shrink; target <150 MB.
- Host is RAM-constrained: builds must stay sequential with capped workers (see `docs/DECISIONS.md`).
- No git commits yet; repository is untracked.

## Recommended next steps

1. Commit the current Phase-0 baseline (suggested: initial commit).
2. Complete Phase 1: migrations (add `schemaLocation` + `AutoMigration`), `core:workmanager` module + `MetadataScanWorker`, wire persistable-permission state, add `MediaStoreFileDiscoverySource`.
3. Complete Phase 2: `DeepProcessingWorker` driving `ContentExtractorImpl` → DAO updates, plus scanned-PDF OCR fallback.
4. Then Phases 3–6 (classification/albums, embeddings, hybrid search, intelligence), each with its UI + tests.
5. Phase 7 hardening (tests, lint, profiling on the 4 GB reference device).
