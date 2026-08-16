package com.smartfiles.data.albums

/**
 * Curated per-category keyword dictionaries (LLD §4.3, spec §2). Pure JVM and
 * side-effect free so it is unit-testable. This is the seed taxonomy: the
 * top-level albums the app ensures exist (LLD §4.5), each with its keyword
 * vocabulary. "Photos" has no keywords — it is driven by media type (images
 * with no extractable text), resolved in [ClassificationEngineImpl].
 */
object CategoryLexicon {

    val STOP_WORDS: Set<String> = setOf(
        "the", "and", "for", "with", "this", "that", "from", "have", "was", "were",
        "are", "has", "had", "not", "but", "you", "your", "our", "their", "they",
        "them", "will", "would", "can", "could", "should", "shall", "may", "might",
        "must", "about", "into", "over", "under", "between", "after", "before",
        "during", "such", "only", "than", "very", "just", "also", "its", "been",
        "being", "does", "did", "what", "when", "where", "which", "who", "whom",
        "whose", "there", "here", "these", "those", "each", "every", "both", "few",
        "more", "most", "other", "some", "all", "any", "once", "own", "same", "so",
        "then", "too", "really", "please", "thank", "thanks", "dear", "hi", "hello",
        "regards", "etc", "na", "n/a", "page", "pages", "figure", "fig", "table",
        "section", "appendix", "index", "content", "subject", "document", "file",
        "files", "march", "april", "june", "july", "august", "monday", "tuesday",
        "wednesday", "thursday", "friday", "saturday", "sunday", "january", "february",
        "september", "october", "november", "december",
    )

    private val TOKEN_REGEX = Regex("[^a-z0-9+.#-]")

    fun tokens(text: String): List<String> = TOKEN_REGEX
        .split(text.lowercase())
        .filter { it.isNotEmpty() && it.length >= 2 && it !in STOP_WORDS }

    /**
     * Cheap inflection-aware match: exact equality, or one string is a prefix of
     * the other with at most [MAX_SUFFIX] extra chars (handles "invoice"/"invoices",
     * "certificate"/"certificates", multi-word bigrams vs. single-word terms).
     */
    fun termMatches(term: String, token: String): Boolean {
        if (token == term) return true
        val suffix = token.length - term.length
        if (suffix in 1..MAX_SUFFIX && token.startsWith(term)) return true
        val prefix = term.length - token.length
        return prefix in 1..MAX_SUFFIX && term.startsWith(token)
    }

    data class Keyword(val term: String, val weight: Float = 1f)

    data class Category(
        val name: String,
        val displayName: String,
        val emoji: String,
        val keywords: List<Keyword>,
    )

    val TOP_LEVEL: List<Category> = listOf(
        Category(
            name = "Education", displayName = "Education", emoji = "🎓",
            keywords = listOf(
                Keyword("course", 1.2f), Keyword("lecture"), Keyword("syllabus", 1.2f),
                Keyword("syllabi"), Keyword("assignment"), Keyword("homework"),
                Keyword("exam"), Keyword("quiz"), Keyword("textbook"), Keyword("notes"),
                Keyword("handout"), Keyword("lab"), Keyword("thesis", 1.2f),
                Keyword("dissertation"), Keyword("university", 1.5f), Keyword("college", 1.5f),
                Keyword("school"), Keyword("study"), Keyword("student", 1.2f),
                Keyword("professor"), Keyword("degree", 1.2f), Keyword("semester"),
                Keyword("grade"), Keyword("transcript", 1.2f), Keyword("tutorial"),
                Keyword("worksheet"), Keyword("curriculum"), Keyword("module"),
                Keyword("lesson"), Keyword("scholarship", 1.2f), Keyword("fellowship"),
            ),
        ),
        Category(
            name = "Career", displayName = "Career", emoji = "💼",
            keywords = listOf(
                Keyword("resume", 1.5f), Keyword("cv", 1.4f), Keyword("curriculum"),
                Keyword("job", 1.2f), Keyword("career"), Keyword("offer letter", 1.3f),
                Keyword("interview"), Keyword("salary"), Keyword("payroll", 1.5f),
                Keyword("benefits"), Keyword("onboarding"), Keyword("training"),
                Keyword("performance review", 1.2f), Keyword("appraisal"), Keyword("offer"),
                Keyword("employment", 1.2f), Keyword("employee"), Keyword("referral"),
                Keyword("portfolio"), Keyword("nondisclosure", 1.2f), Keyword("nda"),
                Keyword("professional"), Keyword("recruit"), Keyword("hire"),
            ),
        ),
        Category(
            name = "Finance", displayName = "Finance", emoji = "💰",
            keywords = listOf(
                Keyword("invoice", 1.5f), Keyword("receipt", 1.5f), Keyword("tax", 1.3f),
                Keyword("bank statement", 1.3f), Keyword("mortgage"), Keyword("loan", 1.2f),
                Keyword("credit"), Keyword("debit"), Keyword("balance"), Keyword("payment", 1.2f),
                Keyword("billing"), Keyword("bill", 1.2f), Keyword("salary slip", 1.3f),
                Keyword("pay stub", 1.3f), Keyword("financial", 1.2f), Keyword("finance"),
                Keyword("budget"), Keyword("investment", 1.2f), Keyword("stock"), Keyword("shares"),
                Keyword("mutual fund", 1.2f), Keyword("insurance", 1.2f), Keyword("premium"),
                Keyword("return filing", 1.2f), Keyword("gst", 1.2f), Keyword("vat"),
                Keyword("income"), Keyword("expense", 1.2f), Keyword("transaction"),
                Keyword("interest"), Keyword("reimbursement", 1.2f), Keyword("claim"),
            ),
        ),
        Category(
            name = "Identity", displayName = "Identity", emoji = "🪪",
            keywords = listOf(
                Keyword("passport", 1.5f), Keyword("aadhaar", 1.5f), Keyword("pan card", 1.5f),
                Keyword("license", 1.3f), Keyword("driver"), Keyword("voter"), Keyword("visa", 1.3f),
                Keyword("certificate", 1.2f), Keyword("birth"), Keyword("citizenship"),
                Keyword("national identity", 1.3f), Keyword("identity"), Keyword("identification"),
                Keyword("ration card", 1.3f), Keyword("social security", 1.3f),
                Keyword("abn", 1.3f), Keyword("tfn", 1.3f), Keyword("sin", 1.3f),
            ),
        ),
        Category(name = "Photos", displayName = "Photos", emoji = "🖼️", keywords = emptyList()),
        Category(name = "Uncategorized", displayName = "Uncategorized", emoji = "🗂️", keywords = emptyList()),
    )

    /** Category by stable name, if present. */
    fun byName(name: String): Category? = TOP_LEVEL.firstOrNull { it.name.equals(name, ignoreCase = true) }

    private const val MAX_SUFFIX = 3
}