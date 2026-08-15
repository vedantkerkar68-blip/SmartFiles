package com.smartfiles.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.smartfiles.core.datastore.SettingsDataStore
import com.smartfiles.domain.AppSettings
import com.smartfiles.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Preferences DataStore-backed [SettingsRepository] (LLD §13). */
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data.data.map { fromPrefs(it) }

    override suspend fun get(): AppSettings = fromPrefs(dataStore.data.data.first())

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.data.edit { prefs ->
            val next = transform(fromPrefs(prefs))
            prefs[Keys.AUTO_CLASSIFY] = next.autoClassifyThreshold
            prefs[Keys.SUGGEST] = next.suggestThreshold
            prefs[Keys.MIN_CLUSTER] = next.newAlbumMinClusterSize
            prefs[Keys.CO_HESION] = next.newAlbumCohesionThreshold
            prefs[Keys.DISTINCT] = next.newAlbumDistinctivenessThreshold
            prefs[Keys.PHASH_NEAR] = next.duplicatePhashHammingNear
            prefs[Keys.PHASH_SIMILAR] = next.duplicatePhashHammingSimilar
            prefs[Keys.VERSION_COSINE] = next.versionCosineThreshold
            prefs[Keys.RELATED_MIN] = next.relatedMinSimilarity
            prefs[Keys.RELATED_TOP_K] = next.relatedTopK
            prefs[Keys.W_SEMANTIC] = next.searchWeightSemantic
            prefs[Keys.W_KEYWORD] = next.searchWeightKeyword
            prefs[Keys.W_FILENAME] = next.searchWeightFilename
            prefs[Keys.W_METADATA] = next.searchWeightMetadata
            prefs[Keys.EMBED_DIM] = next.embeddingDim
            prefs[Keys.IDLE_UNLOAD] = next.modelIdleUnloadMs
            prefs[Keys.BATCH_BATTERY] = next.deepBatchBattery
            prefs[Keys.BATCH_CHARGING] = next.deepBatchCharging
            prefs[Keys.VECTOR_CHUNK] = next.vectorSearchChunkSize
            prefs[Keys.PDF_MIN_CHARS] = next.pdfMinCharsPerPage
            prefs[Keys.OCR_MAX_PAGES] = next.ocrMaxPagesPerDoc
            prefs[Keys.CORRECTION_LR] = next.correctionLearningRate
            prefs[Keys.CLOUD] = next.cloudEnabled
        }
    }

    private fun fromPrefs(prefs: Preferences): AppSettings = AppSettings(
        autoClassifyThreshold = prefs[Keys.AUTO_CLASSIFY] ?: AppSettings().autoClassifyThreshold,
        suggestThreshold = prefs[Keys.SUGGEST] ?: AppSettings().suggestThreshold,
        newAlbumMinClusterSize = prefs[Keys.MIN_CLUSTER] ?: AppSettings().newAlbumMinClusterSize,
        newAlbumCohesionThreshold = prefs[Keys.CO_HESION] ?: AppSettings().newAlbumCohesionThreshold,
        newAlbumDistinctivenessThreshold = prefs[Keys.DISTINCT] ?: AppSettings().newAlbumDistinctivenessThreshold,
        duplicatePhashHammingNear = prefs[Keys.PHASH_NEAR] ?: AppSettings().duplicatePhashHammingNear,
        duplicatePhashHammingSimilar = prefs[Keys.PHASH_SIMILAR] ?: AppSettings().duplicatePhashHammingSimilar,
        versionCosineThreshold = prefs[Keys.VERSION_COSINE] ?: AppSettings().versionCosineThreshold,
        relatedMinSimilarity = prefs[Keys.RELATED_MIN] ?: AppSettings().relatedMinSimilarity,
        relatedTopK = prefs[Keys.RELATED_TOP_K] ?: AppSettings().relatedTopK,
        searchWeightSemantic = prefs[Keys.W_SEMANTIC] ?: AppSettings().searchWeightSemantic,
        searchWeightKeyword = prefs[Keys.W_KEYWORD] ?: AppSettings().searchWeightKeyword,
        searchWeightFilename = prefs[Keys.W_FILENAME] ?: AppSettings().searchWeightFilename,
        searchWeightMetadata = prefs[Keys.W_METADATA] ?: AppSettings().searchWeightMetadata,
        embeddingDim = prefs[Keys.EMBED_DIM] ?: AppSettings().embeddingDim,
        modelIdleUnloadMs = prefs[Keys.IDLE_UNLOAD] ?: AppSettings().modelIdleUnloadMs,
        deepBatchBattery = prefs[Keys.BATCH_BATTERY] ?: AppSettings().deepBatchBattery,
        deepBatchCharging = prefs[Keys.BATCH_CHARGING] ?: AppSettings().deepBatchCharging,
        vectorSearchChunkSize = prefs[Keys.VECTOR_CHUNK] ?: AppSettings().vectorSearchChunkSize,
        pdfMinCharsPerPage = prefs[Keys.PDF_MIN_CHARS] ?: AppSettings().pdfMinCharsPerPage,
        ocrMaxPagesPerDoc = prefs[Keys.OCR_MAX_PAGES] ?: AppSettings().ocrMaxPagesPerDoc,
        correctionLearningRate = prefs[Keys.CORRECTION_LR] ?: AppSettings().correctionLearningRate,
        cloudEnabled = prefs[Keys.CLOUD] ?: AppSettings().cloudEnabled,
    )
}

private object Keys {
    val AUTO_CLASSIFY = floatPreferencesKey("auto_classify_threshold")
    val SUGGEST = floatPreferencesKey("suggest_threshold")
    val MIN_CLUSTER = intPreferencesKey("new_album_min_cluster")
    val CO_HESION = floatPreferencesKey("new_album_cohesion")
    val DISTINCT = floatPreferencesKey("new_album_distinctiveness")
    val PHASH_NEAR = intPreferencesKey("duplicate_phash_near")
    val PHASH_SIMILAR = intPreferencesKey("duplicate_phash_similar")
    val VERSION_COSINE = floatPreferencesKey("version_cosine")
    val RELATED_MIN = floatPreferencesKey("related_min_similarity")
    val RELATED_TOP_K = intPreferencesKey("related_top_k")
    val W_SEMANTIC = floatPreferencesKey("weight_semantic")
    val W_KEYWORD = floatPreferencesKey("weight_keyword")
    val W_FILENAME = floatPreferencesKey("weight_filename")
    val W_METADATA = floatPreferencesKey("weight_metadata")
    val EMBED_DIM = intPreferencesKey("embedding_dim")
    val IDLE_UNLOAD = longPreferencesKey("model_idle_unload_ms")
    val BATCH_BATTERY = intPreferencesKey("deep_batch_battery")
    val BATCH_CHARGING = intPreferencesKey("deep_batch_charging")
    val VECTOR_CHUNK = intPreferencesKey("vector_chunk_size")
    val PDF_MIN_CHARS = intPreferencesKey("pdf_min_chars")
    val OCR_MAX_PAGES = intPreferencesKey("ocr_max_pages")
    val CORRECTION_LR = floatPreferencesKey("correction_lr")
    val CLOUD = booleanPreferencesKey("cloud_enabled")
}
