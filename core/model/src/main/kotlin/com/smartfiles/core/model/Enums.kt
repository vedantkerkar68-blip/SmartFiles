package com.smartfiles.core.model

/** File processing lifecycle, per LLD §2.2 / §7 state machine. */
enum class ProcessingStatus { DISCOVERED, METADATA_DONE, CONTENT_DONE, CLASSIFIED, EMBEDDED, INDEXED, FAILED }

enum class DocType { PDF, IMAGE, DOCX, OTHER }

enum class AlbumType { PREDEFINED, AUTO_CREATED, USER_CREATED }

enum class AssignmentSource { AUTO, USER, SUGGESTED_ACCEPTED }

enum class QueueStatus { PENDING, IN_PROGRESS, FAILED, DONE }

enum class CorrectionType { RECLASSIFY, TAG_ADD, TAG_REMOVE, DUPLICATE_REJECTED, ALBUM_MERGE }

enum class DuplicateGroupType { EXACT, PERCEPTUAL_NEAR, SEMANTIC_VERSION }

enum class DuplicateGroupStatus { PENDING_REVIEW, RESOLVED, DISMISSED }
