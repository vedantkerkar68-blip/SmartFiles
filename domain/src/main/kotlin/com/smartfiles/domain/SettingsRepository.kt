package com.smartfiles.domain

import kotlinx.coroutines.flow.Flow

/** User-facing configuration surfaced through Settings (LLD §13). */
data class AppSettings(
    val autoClassifyThreshold: Float = 0.85f,
    val suggestThreshold: Float = 0.60f,
    val newAlbumMinClusterSize: Int = 5,
    val newAlbumCohesionThreshold: Float = 0.78f,
    val newAlbumDistinctivenessThreshold: Float = 0.35f,
    val duplicatePhashHammingNear: Int = 5,
    val duplicatePhashHammingSimilar: Int = 10,
    val versionCosineThreshold: Float = 0.92f,
    val relatedMinSimilarity: Float = 0.55f,
    val relatedTopK: Int = 8,
    val searchWeightSemantic: Float = 0.40f,
    val searchWeightKeyword: Float = 0.30f,
    val searchWeightFilename: Float = 0.20f,
    val searchWeightMetadata: Float = 0.10f,
    val embeddingDim: Int = 384,
    val modelIdleUnloadMs: Long = 60_000,
    val deepBatchBattery: Int = 5,
    val deepBatchCharging: Int = 20,
    val vectorSearchChunkSize: Int = 2_000,
    val pdfMinCharsPerPage: Int = 40,
    val ocrMaxPagesPerDoc: Int = 20,
    val correctionLearningRate: Float = 0.05f,
    val cloudEnabled: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
