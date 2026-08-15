# SmartFiles — Production-Quality Android Implementation Task

You are the primary senior Android engineer responsible for implementing the **SmartFiles — Local AI File Organizer & Semantic Search** application.

I have attached:

1. `SmartFiles-HLD.md`
2. `SmartFiles-LLD.md`

These documents are the **primary engineering specification and source of truth for this implementation**.

Your goal is NOT to produce a demo, mockup, toy implementation, proof of concept, or code that merely looks complete.

Your goal is to build a **real, runnable, maintainable Android application** that follows the architecture and requirements in the attached HLD and LLD and can actually be installed and tested on a physical Android device.

---

# 1. READ BEFORE CODING

Before changing or creating any code:

1. Read the entire `SmartFiles-HLD.md`.
2. Read the entire `SmartFiles-LLD.md`.
3. Inspect the entire repository/workspace.
4. Determine whether an Android project already exists.
5. Inspect:
   - Gradle configuration
   - Android Gradle Plugin version
   - Kotlin version
   - compileSdk / targetSdk / minSdk
   - existing dependencies
   - package structure
   - manifests
   - resources
   - tests
   - build configuration
   - existing architecture
6. Build the project before making major changes if possible.
7. Identify inconsistencies between the existing repository and HLD/LLD.
8. Do not blindly overwrite existing working code.
9. Reuse existing correct code where appropriate.

The HLD and LLD are the requirements.

Do NOT replace their architecture with your preferred architecture simply because you personally prefer another approach.

However, if a requirement or implementation detail is technically invalid, obsolete, incompatible with the current Android toolchain, or impossible as written, do not blindly implement broken code.

Instead:

- identify the issue,
- determine the smallest technically correct solution,
- document the deviation,
- implement the corrected solution,
- keep the intended behavior and architectural boundary intact.

---

# 2. PRIMARY PRODUCT OBJECTIVE

Build SmartFiles as a **virtual intelligent file organization layer** over the user's existing local files.

The application must:

- discover accessible files,
- index metadata,
- detect changes incrementally,
- extract PDF/document/image content,
- perform OCR when required,
- classify files,
- generate tags,
- generate compact embeddings,
- create virtual albums,
- perform hybrid keyword + semantic search,
- show related files,
- detect exact/near/version duplicates,
- learn from user corrections,
- process expensive operations in the background,
- remain usable while indexing,
- avoid modifying or moving original files,
- work offline by default,
- protect user privacy,
- degrade gracefully when ML capabilities are unavailable.

The filesystem is NOT the application's database.

The Room database is the application's virtual organization/index layer.

Never physically move, rename, copy, or reorganize the user's original files unless a future specification explicitly introduces such functionality.

---

# 3. NON-NEGOTIABLE ARCHITECTURE

Follow the architecture defined in the HLD/LLD:

- Kotlin
- Jetpack Compose
- Clean Architecture
- MVVM
- Hilt
- Room + SQLite
- SQLite FTS
- Preferences DataStore
- SAF
- MediaStore
- Photo Picker where appropriate
- PdfBox-Android or the currently verified compatible PDF implementation
- ML Kit for OCR/image labeling where supported
- LiteRT-compatible quantized embedding model
- WorkManager
- Kotlin Coroutines
- SHA-256
- perceptual hashing
- chunked brute-force vector search for V1
- optional cloud strategy behind an interface
- no backend required for V1

Respect the module boundaries defined in the HLD.

The intended structure is conceptually:

app
core
domain
data
feature

with feature modules/screens not directly depending on database implementation details.

AI/ML implementations must remain behind interfaces.

Examples:

- `ContentExtractor`
- `EmbeddingModelManager`
- `ClassificationEngine`
- `EmbeddingRepository`
- `SearchRepository`
- `ClassificationStrategy`

Do not allow ViewModels or UI classes to directly manipulate Room, LiteRT, ML Kit, SAF traversal, or low-level file processing.

---

# 4. IMPORTANT: VERIFY LIBRARY/APIs BEFORE IMPLEMENTING

Do not hallucinate APIs.

Before implementing Android/ML dependencies, verify that the selected versions and APIs actually exist and work together with the current project configuration.

In particular verify:

- current Kotlin compatibility
- Android Gradle Plugin compatibility
- Compose compatibility
- Room version
- Hilt version
- WorkManager version
- ML Kit APIs
- LiteRT APIs
- PDF extraction library compatibility
- Android 8.0+ compatibility
- FTS implementation supported by Room/current SQLite
- required manifest declarations
- foreground worker requirements
- SAF APIs
- MediaStore APIs

Prefer stable APIs over experimental APIs unless the requirement genuinely needs an experimental API.

Do not introduce a dependency merely because it makes implementation easier.

Every dependency must have a concrete reason.

---

# 5. PONYTAIL ENGINEERING PRINCIPLE

Use the engineering principles of:

**DietrichGebert/ponytail**

Repository:

https://github.com/DietrichGebert/ponytail

The Ponytail philosophy is:

1. Does this need to exist?
2. Does the codebase already provide it?
3. Does the standard library provide it?
4. Does Android provide it natively?
5. Does an already-installed dependency provide it?
6. Can the solution be significantly smaller?
7. Only then write additional code.

Use this principle to prevent:

- unnecessary abstractions,
- speculative frameworks,
- duplicate utilities,
- unnecessary dependencies,
- over-engineered caching,
- unnecessary wrappers,
- excessive design patterns,
- giant helper classes,
- duplicated Android functionality,
- unnecessary AI components.

But Ponytail does NOT mean:

- removing validation,
- removing error handling,
- removing security,
- removing accessibility,
- removing tests,
- removing permission handling,
- ignoring lifecycle behavior,
- ignoring data corruption,
- ignoring cancellation,
- ignoring memory limits,
- ignoring privacy,
- replacing required architecture with shortcuts.

**Lazy about unnecessary code. Never lazy about correctness.**

Use the minimum implementation that satisfies the actual specification.

---

# 6. DO NOT BUILD AN UNNECESSARY LOCAL LLM

Do NOT introduce a local LLM.

The HLD explicitly excludes a local LLM and large BERT-class model from V1.

Do not add:

- Gemma
- Llama
- Mistral
- Qwen
- large transformer chat models
- local generative AI
- unnecessary agent frameworks

unless the specification is explicitly changed later.

SmartFiles is primarily:

**rules + metadata + OCR + embeddings + similarity + classification + ranking**

not a chatbot.

Natural-language search must use the lightweight query parsing approach specified in the LLD.

---

# 7. PERFORMANCE IS A HARD REQUIREMENT

The application must be designed for approximately:

**4 GB RAM Android devices**

The HLD target is:

- background steady-state RAM < 200 MB
- ideally <150 MB
- cold start <1.5 seconds
- keyword search around <150 ms at 20k files
- hybrid search around <400 ms at 20k files
- APK target <150 MB including the quantized model

These are engineering targets that must be measured, not assumed.

Do NOT implement something and simply claim it is lightweight.

Measure it.

---

# 8. MEMORY RULES

Never load the entire file corpus into memory.

Never load all embeddings into memory.

Never retain full-resolution images unnecessarily.

Never process hundreds of files concurrently.

Never keep the embedding model resident permanently.

Follow the LLD design:

- bounded vector-search chunks
- one in-flight embedding inference
- lazy model loading
- model idle unloading
- downsampled image processing
- bounded OCR pages
- WorkManager batching
- cancellation support

Embedding vectors must use compact storage as specified.

The embedding model lifecycle must support:

load → use → idle timeout → unload

Default model idle timeout:

`60 seconds`

---

# 9. PROCESSING PIPELINE

Implement the pipeline as three levels.

## Level 1 — Cheap

Metadata:

- URI
- filename
- MIME type
- size
- modification time
- dimensions where applicable

Only perform expensive hashing when the file is new/changed according to the specified change-detection logic.

Do NOT calculate SHA-256 for every unchanged file during every scan.

Use cached metadata to avoid unnecessary work.

## Level 2 — Moderate

Perform:

- PDF text extraction
- OCR fallback
- image OCR
- image labeling
- perceptual hashing where appropriate

Only process new/changed files.

## Level 3 — Expensive

Perform:

- embedding generation
- classification
- clustering
- album assignment
- other expensive intelligence

Only promote files after previous processing levels succeed.

The app must be usable while Level 2/3 processing continues.

---

# 10. FILE ACCESS

Use:

- Storage Access Framework
- MediaStore
- Photo Picker where appropriate

Do NOT require:

`MANAGE_EXTERNAL_STORAGE`

Do not bypass Android storage security.

Persist folder permissions where supported.

Handle revoked permissions gracefully.

If a permission disappears:

- do not delete the existing index,
- mark the folder as needing permission,
- notify the user,
- allow re-grant.

Existing indexed information must survive permission loss.

---

# 11. ORIGINAL FILES ARE SACRED

SmartFiles must never:

- rename originals,
- move originals,
- delete originals,
- duplicate originals,
- rewrite originals.

Store only derived information:

- metadata
- extracted text
- embeddings
- classifications
- tags
- thumbnails only when genuinely necessary

The virtual album hierarchy exists inside Room.

---

# 12. DATABASE

Implement the database from the LLD.

Respect the specified entities and relationships, including:

- File
- Album
- FileAlbumCrossRef
- Tag
- FileTagCrossRef
- Embedding
- ProcessingQueue
- UserCorrection
- DuplicateGroup
- DuplicateGroupMember
- FTS/search structures

Use proper:

- foreign keys
- indexes
- unique constraints
- transactions
- migrations
- type converters

Do not blindly use `fallbackToDestructiveMigration`.

Database migrations must preserve user data.

Embedding model versions must remain explicit so model upgrades can happen incrementally.

---

# 13. SEARCH

Implement hybrid search exactly according to the LLD concept.

Search should combine:

- keyword relevance
- semantic similarity
- filename relevance
- metadata relevance

Default weights:

- semantic = 0.40
- keyword = 0.30
- filename = 0.20
- metadata = 0.10

Do not return results purely because the system needs to fill a list.

The ranking system must be deterministic and explainable.

Every result should be able to expose useful "why this matched" information.

Example:

"Matched because:
- semantic similarity: high
- filename: partial match
- album: Education → Java
- tags: inheritance, polymorphism"

---

# 14. SEMANTIC SEARCH

Use the specified compact embedding approach.

Target:

- approximately 384 dimensions
- quantized model
- compact storage
- normalized vectors

Use bounded-memory chunked vector search.

Do NOT implement an ANN/vector database for V1 unless benchmarking proves the existing approach is inadequate.

The LLD deliberately keeps ANN as a future extension.

Do not prematurely optimize.

---

# 15. AUTOMATIC ALBUMS

This is one of the core features.

Use confidence gates.

Default:

- >85% → automatic assignment
- 60–85% → user suggestion
- <60% → Uncategorized

Never automatically create arbitrary albums from one file.

Dynamic album creation must require evidence from a cluster.

Default:

- auto-create candidate:
  - at least 5 files
  - cohesion >= 0.78
  - distinctiveness >= 0.35

Medium-confidence clusters become suggestions.

Weak clusters remain searchable without creating an album.

Do not create meaningless album explosions such as:

- "Java"
- "Java Notes"
- "Java PDF"
- "Java Documents"
- "Java Study"
- "Java Stuff"

unless the evidence actually supports distinct clusters.

---

# 16. CLASSIFICATION

Use the LLD scoring model:

- keyword score: 45%
- embedding-to-centroid: 35%
- cluster agreement: 15%
- user history: 5%

Make the scoring components inspectable.

Do not use an LLM simply to decide which folder a file belongs to.

The system must be explainable.

---

# 17. USER CORRECTIONS ARE IMPORTANT

When the user corrects an automatic classification:

1. Immediately pin the corrected assignment.
2. Prevent silent reclassification from undoing it.
3. Update the relevant centroid using the specified lightweight learning rule.
4. Update lightweight keyword associations.
5. Do NOT retrain a neural model.

The system should improve through lightweight statistics, not through an expensive training pipeline.

---

# 18. DUPLICATE DETECTION

Implement:

### Exact duplicates

SHA-256.

### Near-image duplicates

Perceptual hash.

### Document versions

Filename/version pattern + embedding similarity.

Use the specified thresholds.

Never automatically delete a duplicate.

Every duplicate result must go through a user-review flow.

"Not a duplicate" must be remembered to prevent repeatedly flagging the same pair.

---

# 19. RELATED FILES

Implement nearest-neighbor related-file retrieval.

Use the LLD's metadata boosts where appropriate.

Respect the minimum similarity threshold.

If there are only 2 genuinely related files, show 2.

Do not fabricate 8 related files simply because the UI has space for 8.

---

# 20. BACKGROUND PROCESSING

Use WorkManager.

Implement separate responsibilities for:

- metadata scanning
- deep processing
- user-triggered processing
- duplicate scanning
- permission validation

Respect:

- battery constraints
- charging constraints
- idle constraints
- retries
- exponential/backoff behavior
- cancellation
- process death
- resumability

The queue must be persistent.

If Android kills the process halfway through a file:

the next run should continue safely rather than corrupting state or starting the entire corpus again.

---

# 21. CONCURRENCY

Use Kotlin Coroutines correctly.

Follow the LLD threading model:

`Dispatchers.IO`
→ file I/O, Room

`Dispatchers.Default`
→ CPU work

single limited-parallelism inference dispatcher
→ ML Kit/LiteRT

Main
→ UI only

Do not create a large coroutine for every file.

Do not use unbounded `async`.

Do not run multiple embedding interpreters simultaneously.

Do not introduce thread pools unless profiling proves they are necessary.

---

# 22. FAILURE HANDLING

Every subsystem must fail gracefully.

Examples:

Corrupt PDF:

→ preserve metadata  
→ mark processing failure  
→ allow search by filename  
→ retry with backoff

OCR failure:

→ preserve metadata  
→ continue where possible

Embedding failure:

→ keyword search must continue working

SAF permission failure:

→ preserve index  
→ show re-grant state

ML unavailable:

→ degrade to non-ML functionality

Cloud failure:

→ local functionality must continue

Out-of-memory during embedding:

→ catch safely  
→ unload/release model  
→ record failure  
→ retry later under safer conditions

No single corrupted file should stop the entire indexing queue.

---

# 23. PRIVACY

Privacy is a product requirement, not an optional enhancement.

Default:

**100% local processing**

No analytics.

No telemetry.

No silent network calls.

No uploading files.

If optional cloud functionality is implemented:

- explicit user opt-in
- clear UI disclosure
- extracted text only where allowed
- never raw files/images by default
- HTTPS/TLS
- visible audit information
- local processing remains available

Do not introduce Firebase Analytics or similar telemetry automatically.

---

# 24. SECURITY

Implement:

- secure URI handling
- permission validation
- no unsafe file paths
- no arbitrary shell execution
- no cleartext network traffic
- secure preference handling
- Android Keystore where encryption keys are required
- proper input validation
- safe database queries
- no sensitive information in logs

Never log:

- full extracted documents
- personal document contents
- access tokens
- API keys
- private URIs unnecessarily

If SQLCipher/encrypted database support is not required for V1, preserve the architectural seam for it rather than forcing an unnecessary dependency.

---

# 25. UI

Build a real usable UI, not placeholder screens.

Required areas:

- onboarding
- home / Smart Files
- search
- album browser
- file details
- related files
- duplicate review
- settings
- processing status
- suggestions/corrections
- permission state

UI must:

- work on small screens
- support loading states
- support empty states
- support errors
- avoid blocking the main thread
- show background indexing progress
- allow cancellation where appropriate
- provide accessible controls
- handle large file collections using lazy lists

Do not fill the UI with fake statistics.

Everything displayed should come from actual application state.

---

# 26. SETTINGS

Expose appropriate tunables from the LLD through DataStore.

Examples include:

- automatic classification threshold
- suggestion threshold
- duplicate thresholds
- search weights
- processing preferences
- cloud toggle
- indexed folders
- model status
- processing controls

Do not expose dangerous/internal implementation details merely because they exist in the configuration.

Settings should be understandable to normal users.

---

# 27. TESTING IS PART OF IMPLEMENTATION

Do not finish when the code compiles.

Create meaningful tests.

At minimum:

### Unit tests

- confidence scoring
- album threshold decisions
- dynamic album creation
- query parser
- score fusion
- change detection
- duplicate thresholds
- correction learning
- embedding codec

### Database tests

- Room entities
- migrations
- relations
- FTS search
- queue behavior

### Worker tests

- retries
- cancellation
- partial processing
- constraints
- resumability

### Storage tests

Use a test DocumentProvider where practical.

### ML tests

Use deterministic/golden fixtures where possible.

### Performance

Test with synthetic datasets representing:

- 10,000 files
- 20,000 files
- 50,000 files

Measure:

- RAM
- search latency
- indexing throughput
- cold start
- database size
- model memory
- battery-sensitive processing behavior

---

# 28. 4 GB RAM DEVICE IS THE PRIMARY TEST TARGET

Do not develop only on a powerful emulator and assume the application will work on 4 GB devices.

Treat the 4 GB device as the primary constraint.

Avoid:

- huge object graphs
- unnecessary caches
- multiple image copies
- full-corpus embedding arrays
- large in-memory search indexes
- simultaneous OCR jobs
- simultaneous embedding jobs
- long-lived model instances

If a feature causes unacceptable memory usage, redesign the implementation rather than simply increasing memory.

---

# 29. IMPLEMENTATION ORDER

Do NOT attempt to write the entire application in one giant change.

Implement in vertical, testable phases.

## Phase 0 — Repository and build foundation

Create/verify:

- Gradle
- modules
- dependency management
- Hilt
- Compose
- navigation
- testing
- lint
- formatting
- baseline architecture

Goal:

**Clean build.**

---

## Phase 1 — Storage + database

Implement:

- SAF
- MediaStore
- folder grants
- Room
- entities
- DAOs
- migrations
- basic metadata indexing
- processing queue

Goal:

User selects a folder and real files appear in the local database.

---

## Phase 2 — Content extraction

Implement:

- PDF text extraction
- OCR fallback
- image OCR
- image labels
- processing status
- error handling

Goal:

Real file content becomes searchable metadata.

---

## Phase 3 — Classification + albums

Implement:

- category lexicon
- scoring
- confidence gates
- album hierarchy
- dynamic album candidates
- suggestions
- corrections

Goal:

Real files are automatically organized into meaningful virtual albums.

---

## Phase 4 — Embeddings + semantic search

Implement:

- model manager
- embedding generation
- model lifecycle
- compact vector storage
- chunked vector search
- query embedding

Goal:

"Find my Java inheritance notes" can find relevant files even when the exact words do not appear in the filename.

---

## Phase 5 — Hybrid search

Implement:

- FTS
- semantic retrieval
- candidate union
- score fusion
- ranking
- explanation signals
- query parsing

Goal:

Fast, useful, explainable search.

---

## Phase 6 — Intelligence

Implement:

- duplicate detection
- related files
- personalization
- correction learning

Goal:

Complete core SmartFiles intelligence.

---

## Phase 7 — Hardening

Perform:

- performance profiling
- memory profiling
- battery testing
- crash testing
- permission testing
- process-death testing
- database migration testing
- large-corpus testing
- UI testing
- accessibility checks

Only after this should you consider the implementation complete.

---

# 30. BUILD/TEST/VERIFY LOOP

After each meaningful implementation phase:

1. compile
2. run unit tests
3. run relevant instrumented tests
4. inspect compiler warnings
5. inspect lint
6. inspect generated APK
7. inspect runtime behavior where possible
8. fix failures
9. review the diff
10. remove unnecessary code
11. continue

Do not accumulate hundreds of unverified changes and then attempt one final build.

---

# 31. AGENT SELF-REVIEW LOOP

Before declaring any phase complete, perform this loop:

### Step A — Requirement check

Compare implementation against HLD/LLD.

Ask:

- What requirement did I miss?
- What requirement did I accidentally change?
- Did I implement unnecessary functionality?

### Step B — Architecture check

Check:

- dependency direction
- module boundaries
- Android dependencies leaking into domain
- repository boundaries
- ViewModel responsibilities
- DI correctness

### Step C — Correctness check

Check:

- nullability
- lifecycle
- concurrency
- cancellation
- process death
- database transactions
- permission revocation
- corrupted files
- model failures

### Step D — Performance check

Check:

- memory
- file I/O
- database queries
- model lifecycle
- image allocation
- vector search
- unnecessary recomposition
- background work

### Step E — Ponytail review

Ask:

> "What code did I write that does not actually need to exist?"

Delete unnecessary code.

But never delete:

- security
- validation
- accessibility
- data-loss protection
- required tests
- error handling

---

# 32. PONYTAIL REVIEW COMMAND

If Ponytail is installed in OpenCode, use its review/audit capability when appropriate.

After major milestones:

`/ponytail-review`

For a broader repository audit:

`/ponytail-audit`

Use the results to identify unnecessary complexity.

Do not blindly apply every suggested deletion.

The HLD/LLD requirements have higher priority than Ponytail's optimization suggestions.

---

# 33. DOCUMENTATION

Maintain a small engineering record.

Create/update:

`docs/IMPLEMENTATION_STATUS.md`

Track:

- completed requirements
- current phase
- known issues
- architectural deviations
- performance measurements
- unsupported devices/APIs
- remaining work

Create/update:

`docs/DECISIONS.md`

Only record meaningful architectural decisions.

Do not create documentation for the sake of creating documentation.

---

# 34. REQUIREMENT TRACEABILITY

Maintain a mapping between:

HLD/LLD requirement

→ implementation

→ test

For example:

`LLD §4.6 Hybrid Search`

→ `HybridSearchOrchestrator`

→ `HybridSearchOrchestratorTest`

This allows us to verify that the final product actually implements the design.

---

# 35. DO NOT FAKE FUNCTIONALITY

This is critical.

Never implement fake:

- embeddings
- OCR
- classification
- search results
- duplicate detection
- album generation
- processing progress
- ML status

Do not use hardcoded demo files.

Do not use fake repository data after the real subsystem is available.

Do not write TODO comments instead of implementing required functionality unless the requirement is explicitly deferred.

If a required external model/file/API cannot currently be included, create the correct abstraction and clearly report the blocker rather than pretending it works.

---

# 36. MODEL FILE HANDLING

For the embedding model:

- do not commit an enormous model accidentally,
- verify licensing,
- verify Android/LiteRT compatibility,
- verify model input/output dimensions,
- verify quantization,
- verify memory footprint,
- verify inference time on the reference device.

Do not assume that a model described as "384-dimensional" is automatically compatible with the embedding code.

Validate it.

---

# 37. NO PREMATURE FEATURES

Do NOT implement V2 features simply because they are architecturally possible.

Examples:

- cross-device synchronization
- managed backend
- ANN vector database
- document expiry notification
- handwriting-specialized OCR
- physical file organization
- full cloud AI
- conversational AI

The HLD/LLD deliberately keeps these outside V1.

Build a strong V1 first.

---

# 38. WHEN YOU FIND A DESIGN PROBLEM

Do not silently change the specification.

Use this format internally:

**Issue**

What is wrong?

**Impact**

Why does it matter?

**Smallest correction**

What is the minimal technically correct solution?

**Compatibility**

Does it preserve the original product requirement?

**Implementation**

Apply the correction.

**Documentation**

Record the deviation in `docs/DECISIONS.md`.

Do not ask me about every tiny implementation decision.

Use engineering judgment.

Ask only when there is a genuine product-level ambiguity that cannot safely be resolved from the HLD/LLD.

---

# 39. PRIORITY ORDER

When instructions conflict, use this priority:

1. Security and data safety
2. Correctness
3. Explicit product requirements
4. HLD
5. LLD
6. Android platform constraints
7. Tests and measurable behavior
8. Performance requirements
9. Maintainability
10. Ponytail/YAGNI optimization
11. Cosmetic preferences

Never sacrifice correctness for fewer lines of code.

---

# 40. FINAL DEFINITION OF DONE

Do not say "completed" merely because the source code exists.

SmartFiles is complete only when:

- project builds successfully
- APK can be generated
- app launches
- onboarding works
- folder permissions work
- real files can be indexed
- metadata is persisted
- PDF extraction works
- OCR works where supported
- embeddings work where supported
- classification works
- virtual albums work
- confidence thresholds work
- suggestions work
- user corrections work
- hybrid search works
- related files work
- duplicate detection works
- background processing works
- process death does not corrupt the queue
- permissions can be revoked/re-granted
- failures do not stop the entire pipeline
- no original files are modified
- local-first privacy is preserved
- tests pass
- lint/build checks pass
- memory behavior has been measured
- 4 GB device behavior has been considered/tested
- major requirements are traceable to implementation/tests
- unnecessary complexity has been removed

---

# 41. FIRST ACTION

Do NOT start by generating hundreds of files.

First:

1. inspect the repository,
2. read the HLD completely,
3. read the LLD completely,
4. inspect the existing project,
5. verify the current Android/toolchain constraints,
6. produce a concise implementation plan,
7. identify specification inconsistencies/blockers,
8. establish the smallest buildable foundation,
9. implement Phase 0,
10. build and test it,
11. continue phase-by-phase.

After every phase, verify the actual result before proceeding.

Your job is not to maximize the amount of code produced.

Your job is to maximize **working product functionality per line of code**, while preserving security, reliability, maintainability, performance, and the requirements in the attached HLD and LLD.

Do not optimize for "looking complete."

Optimize for **actually working on a real Android device**.