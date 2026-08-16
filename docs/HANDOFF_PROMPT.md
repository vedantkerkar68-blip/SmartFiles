# Handoff Prompt — SmartFiles

Use this entire prompt when starting a fresh opencode session on another machine
(e.g. after migrating the repo). It carries all standing rules, constraints,
project context, and the immediate next steps.

---

You are working on **SmartFiles**, an Android app that scans local files and
builds a personal knowledge graph (albums, tags, semantic search). Treat the
spec below as truth.

## Standing rules (never violate)

1. **Spec is truth.** `docs/` files + `README.md` module docs define the
   architecture. Follow them; never redesign on a whim.
2. **Architecture is non-negotiable:** Clean Architecture, MVVM, Hilt, Room,
   FTS5, DataStore, SAF, WorkManager, Coroutines/Flow. Keep AI/ML behind
   interfaces. Local-first: zero network calls at runtime.
3. **Never touch originals.** Any user file read by the app is opened
   read-only or copied into app storage; the original is never modified or
   deleted.
4. **Do not fake functionality.** Every feature must genuinely work. If spec
   has an invalid/irrational detail, apply the *smallest correction*, then
   document it in `docs/DECISIONS.md`.
5. **Room is a virtual index only.** It stores metadata/derived data, never a
   copy of file content.
6. **Priority order:** security > correctness > product requirements > HLD >
   LLD > Android constraints > tests > performance > maintainability.
7. **No comments in code unless asked.** Match existing code style, libraries,
   and conventions. Never assume a library exists — check Gradle first.
8. **Docs:** update `docs/DECISIONS.md` when you deviate from spec, and
   `docs/IMPLEMENTATION_STATUS.md` at every phase boundary.
9. **Commits:** only commit/push when the user explicitly asks. After each
   phase: push to remote, commit, then move to the next phase. Never commit
   secrets or `build/` outputs.

## Environment constraints (CRITICAL on this host)

- OS: Windows, PowerShell only. Repo root: the working directory.
- The host is **extremely memory-constrained** (~7.3 GB RAM total, often under
  1 GB free). Gradle builds routinely get OOM-killed.
- **Check RAM first** (`Get-CimInstance Win32_OperatingSystem`) before any
  heavy command. If free RAM < ~1.5 GB, do NOT start Gradle — free memory
  (close idle apps) first.
- Build sequentially, single worker: `.\gradlew.bat <task> --console=plain
  "-Dorg.gradle.workers.max=1"`. Prefer `:app:compileDebugKotlin` until clean,
  then `:app:assembleDebug`.
- Never run parallel/full builds. Gradle JVM heap is capped in
  `gradle.properties` — do not raise it.
- Interrupted builds corrupt `.class` files; Gradle treats them as
  UP-TO-DATE. Fix with a **full `clean` + single `assembleDebug`**, not by
  deleting individual `.class` files.
- Free stale `java`/`kotlin`/`gradle` processes before retries.

## Project state

- Git branch `main`; remote `origin` =
  https://github.com/vedantkerkar68-blip/SmartFiles.git
- Completed & pushed:
  - Phase 1 `1948e28` — scanning/metadata (Room index, MIME, dates, sizes)
  - Phase 2 `44a2e9b` — text extraction + OCR (PDFBox, ML Kit), FTS index
  - Phase 3 `36c11d6` — classification: lexicon-first, virtual albums,
    tags + album suggestions
  - Phase 4 `2461101` — on-device MiniLM-L6 int8 embeddings, album
    centroids, distinctness-gated dynamic album auto-create, worker Level-4
- Phase 4 Kotlin compiles and its tokenizer unit tests pass, but the APK was
  **not** fully assembled on the old host (dedex OOM only). **First task on a
  new machine: verify with a clean single-worker
  `:app:assembleDebug`.**

## Architecture (current)

- Module map: `app` (launcher) / `feature:*` (UI) / `domain` (use cases,
  repos) / `data` (Hilt DI, repos impl, WorkManager, PDFBox/OCR extractors) /
  `core:database` (Room DAOs+DB) / `core:ml` (embeddings, tokenizer,
  representative-text builder; NO Hilt in `core:ml`) /
  `core:filesystem` (SAF/URIs) / `core:datastore` (settings) /
  `core:workmanager` / `core:designsystem`.
- `core:ml`: pure-Kotlin `WordPieceTokenizer` (bundled `vocab.txt`),
  `RepresentativeTextBuilder`, `EmbeddingModelManager` (LiteRT
  `com.google.ai.edge.litert:litert:2.1.0`) exposing `EmbeddingCapabilities`.
- Embeddings stored per `(fileId, modelVersion)` in Room (`EmbeddingEntity`,
  float16 via `EmbeddingCodec`); chunked kNN (`VECTOR_SEARCH_CHUNK_SIZE=2000`,
  no ANN index); album centroids in `albums.centroidEmbedding`.
- `ScanFilesUseCase.TARGET_LEVEL_ALL = 4`; worker levels: 1 metadata, 2
  extract, 3 classify, 4 embed (`ProcessingStatus.EMBEDDED`).
- DI: `DataModule` (providers incl. `EmbeddingModelManager`,
  `RepresentativeTextBuilder`), `AppBindingsModule` (binds
  `EmbeddingRepository` → `EmbeddingRepositoryImpl`).

## Key decisions (superset — see docs/DECISIONS.md)

- Real int8 quantized MiniLM-L6 (21.7 MB tflite) model bundled locally;
  WordPiece is pure-Kotlin (local-first).
- Album clusters above `DistinctnessGate` auto-create ONLY when the cluster’s
  embedding centroid is far enough from existing album centroids; otherwise
  suggest-only.
- Room schema is v2; `fallbackToDestructiveMigration` still active; schema
  migrations deferred to Phase 7.

## Immediate next steps (on the new machine)

1. `Get-CimInstance Win32_OperatingSystem` — check free RAM.
2. Single-worker clean `:app:assembleDebug`; confirm `app/build/outputs/apk/
   debug/app-debug.apk` is NEWER than the Phase-3 build and includes
   `assets/embeddings/` (tflite + vocab).
3. Run `:core:ml:testDebugUnitTest` (tokenizer tests) and any data tests.
4. On success, begin **Phase 5** (per HLD/LLD): hybrid search —
   `SemanticSearchRepository` / `SearchRepositoryImpl` wiring keyword (FTS)
   + embedding (kNN) scores; continue the documented phase workflow.

Keep responses concise; stop and ask when ambiguous.