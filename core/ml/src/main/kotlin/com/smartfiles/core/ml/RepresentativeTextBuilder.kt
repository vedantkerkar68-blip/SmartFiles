package com.smartfiles.core.ml

/**
 * Builds the bounded representative text used as embedding input (LLD §4.4):
 * `filename + first ~500 tokens of extracted text + top extracted tags`, keeping
 * inference cost and memory flat regardless of source-document length.
 */
class RepresentativeTextBuilder {

    fun build(displayName: String, extractedText: String?, tags: List<String>): String = buildString {
        append(displayName)
        if (!extractedText.isNullOrBlank()) {
            append('\n')
            append(extractedText.take(MAX_TEXT_CHARS))
        }
        if (tags.isNotEmpty()) {
            append('\n')
            tags.take(MAX_TAGS).forEach { append(it).append(' ') }
        }
    }

    companion object {
        private const val MAX_TEXT_CHARS = 8_000
        private const val MAX_TAGS = 10
    }
}