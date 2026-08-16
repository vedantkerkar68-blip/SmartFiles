package com.smartfiles.data.albums

import com.smartfiles.core.database.dao.FileDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [CorpusTermIndex] backed by a bounded sample of the user's own extracted text
 * (up to [SAMPLE_DOCS] documents, refreshed every [TTL_MS]). Lazy — it only
 * touches the database the first time tags are requested — so an empty corpus
 * simply yields empty IDF statistics.
 */
@Singleton
class DatabaseCorpusTermIndex @Inject constructor(
    private val fileDao: FileDao,
) : CorpusTermIndex {

    private val mutex = Mutex()
    private var df: Map<String, Int> = emptyMap()
    private var docCount: Int = 0
    private var loadedAt: Long = 0

    override val documentCount: Int get() = docCount

    override fun documentFrequency(term: String): Int = df[term] ?: 0

    override suspend fun ensureLoaded() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (docCount > 0 && now - loadedAt < TTL_MS) return
            val sample = fileDao.sampleExtractedTexts(SAMPLE_DOCS)
            val counts = HashMap<String, Int>()
            for (row in sample) {
                val distinct = CategoryLexicon.tokens(row.extractedText.take(MAX_SAMPLE_CHARS)).toSet()
                for (term in distinct) {
                    if (term.length < 3) continue
                    counts.merge(term, 1, Int::plus)
                }
            }
            df = counts
            docCount = sample.size
            loadedAt = now
        }
    }

    companion object {
        private const val SAMPLE_DOCS = 300
        private const val MAX_SAMPLE_CHARS = 10_000
        private const val TTL_MS = 5 * 60_000L
    }
}