package com.smartfiles.data.albums

/**
 * Bounded vocabulary of corpus-relative term statistics used by [TagExtractor]
 * for TF-IDF over the user's own extracted text (LLD §4.3). Implementations load
 * a bounded sample of documents once and refresh on a TTL.
 */
interface CorpusTermIndex {
    /** Number of sample documents the index was built over. */
    val documentCount: Int
    /** Number of sample documents containing [term]. */
    fun documentFrequency(term: String): Int
    suspend fun ensureLoaded()
}

/**
 * Extracts tags without an LLM (LLD §4.3, spec §2/§17):
 *  1. curated lexicon terms (weighted by category strength),
 *  2. corpus-relative TF-IDF terms outside the lexicon over the user's texts,
 *  3. structured spans (dates) as filterable metadata tokens.
 */
class TagExtractor(
    private val corpus: CorpusTermIndex,
) {
    suspend fun extract(text: String): List<Pair<String, Float>> {
        if (text.isBlank()) return emptyList()
        corpus.ensureLoaded()

        val toks = CategoryLexicon.tokens(text)
        if (toks.isEmpty()) return emptyList()

        val localTermFreq = toks.groupingBy { it }.eachCount()

        val scored = LinkedHashMap<String, Float>()

        // 1. Lexicon terms: any matched term across all categories; weight by
        //    keyword weight and how often it appears.
        for (category in CategoryLexicon.TOP_LEVEL) {
            for (kw in category.keywords) {
                val term = kw.term
                val count = toks.count { CategoryLexicon.termMatches(term, it) }
                if (count > 0) {
                    val boost = 1f + EXP_BOOST * (count - 1).coerceAtMost(3)
                    scored.merge(display(term), kw.weight * boost) { old, new -> maxOf(old, new) }
                }
            }
        }

        // 2. Corpus-relative TF-IDF for meaningful non-lexicon terms.
        if (corpus.documentCount > 0) {
            for ((term, tf) in localTermFreq) {
                if (term in scored || term.length < 4 || isCategoricalNoise(term)) continue
                val df = corpus.documentFrequency(term)
                val idf = Math.log((corpus.documentCount + 1.0) / (df + 1.0)) + 1.0
                if (idf < MIN_IDF) continue
                val score = Math.log(1.0 + tf) * idf
                scored.merge(term, score.toFloat(), ::maxOf)
            }
        }

        // 3. Structured date/year spans -> filterable metadata tags.
        DateSpanExtractor.extract(text).forEach { scored.merge(it, STRUCTURED_TAG_SCORE) { old, new -> maxOf(old, new) } }

        return scored.entries
            .sortedByDescending { it.value }
            .take(MAX_TAGS)
            .map { it.key to it.value }
    }

    private fun display(term: String): String = term.split(' ').joinToString(" ") { w ->
        w.replaceFirstChar { ch -> ch.uppercase() }
    }

    private fun isCategoricalNoise(term: String): Boolean = term.length == 1

    companion object {
        private const val MAX_TAGS = 10
        private const val EXP_BOOST = 0.2f
        private const val MIN_IDF = 1.8
        private const val STRUCTURED_TAG_SCORE = 0.9f
    }
}

/** Local, regex-based date span extraction (no runtime ML model download). */
object DateSpanExtractor {

    private val ISO_DATE = Regex("""\b(19|20)\d{2}[-/.]\d{1,2}[-/.]\d{1,2}\b""")
    private val DASH_DATE = Regex("""\b\d{1,2}[-/.]\d{1,2}[-/.]\b(19|20)\d{2}\b""")
    private val YEAR = Regex("""\b(19|20)\d{2}\b""")

    fun extract(text: String): List<String> {
        val spans = mutableListOf<String>()
        ISO_DATE.findAll(text).forEach { spans += "date:" + it.value.replace('/', '-').replace('.', '-') }
        for (m in DASH_DATE.findAll(text)) {
            val parts = m.value.split(Regex("[-/.]"))
            if (parts.size != 3) continue
            val (a, b, year) = parts
            val month = a.toIntOrNull(); val day = b.toIntOrNull()
            if (month != null && day != null) {
                spans += "date:%04d-%02d-%02d".format(year.toInt(), month, day)
            }
        }
        YEAR.findAll(text).forEach { spans += "year:${it.value}" }
        return spans
    }
}