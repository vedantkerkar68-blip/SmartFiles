package com.smartfiles.core.ml

import java.io.InputStream
import java.text.Normalizer

/**
 * Minimal WordPiece tokenizer matching `sentence-transformers/all-MiniLM-L6-v2`
 * (uncased, accent-stripped), used to feed the bundled int8 LiteRT model.
 *
 * Standard BERT special-token ids: [PAD]=0, [UNK]=100, [CLS]=101, [SEP]=102.
 * [MAX_SEQ] (256) matches the model's positional-embedding capacity (verified
 * at integration time: seq-256 inference returns a unit-norm 384-dim vector).
 */
class WordPieceTokenizer(private val vocab: Map<String, Int>, private val maxSeq: Int = MAX_SEQ) {

    fun tokenIds(text: String): IntArray {
        var len = 0
        val ids = IntArray(maxSeq)
        ids[len++] = vocab[CLS] ?: UNK_ID

        val tokens = normalizedWords(text)
        outer@ for (word in tokens) {
            for (piece in wordPieces(word)) {
                val id = vocab[piece] ?: vocab[UNK] ?: UNK_ID
                ids[len++] = id
                if (len >= maxSeq - 1) break@outer
            }
            if (len >= maxSeq - 1) break
        }

        ids[len++] = vocab[SEP] ?: SEP_ID
        return ids
    }

    /** 1 where a token position is a real word token, 0 where it is padding. */
    fun attentionMask(text: String): IntArray {
        var len = 0
        val mask = IntArray(maxSeq)
        mask[len++] = 1
        val tokens = normalizedWords(text)
        for (word in tokens) {
            for (_piece in wordPieces(word)) {
                if (len >= maxSeq - 1) break
                mask[len++] = 1
            }
            if (len >= maxSeq - 1) break
        }
        mask[len++] = 1
        return mask
    }

    /** Lowercases and strips accents (NFD), matching the uncased model. */
    private fun normalizedWords(text: String): List<String> {
        val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            if (!isCombining(ch)) sb.append(ch)
        }
        return sb.toString().split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
    }

    /** Standard greedy WordPiece with '##' continuation prefix. */
    fun wordPieces(word: String): List<String> {
        if (vocab.containsKey(word)) return listOf(word)
        val pieces = ArrayList<String>(2)
        var start = 0
        while (start < word.length) {
            var end = word.length
            var piece: String? = null
            while (end > start) {
                val candidate = if (start == 0) word.substring(start, end) else "##" + word.substring(start, end)
                if (vocab.containsKey(candidate)) {
                    piece = candidate
                    break
                }
                end--
            }
            if (piece == null) return listOf(UNK)
            pieces.add(piece)
            start = end
        }
        return pieces
    }

    private fun isCombining(ch: Char): Boolean {
        val type = Character.getType(ch)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt()
    }

    companion object {
        const val MAX_SEQ = 256
        const val CLS = "[CLS]"
        const val SEP = "[SEP]"
        const val PAD = "[PAD]"
        const val UNK = "[UNK]"
        const val UNK_ID = 100
        const val SEP_ID = 102
        private val WHITESPACE_REGEX = Regex("\\s+")

        /** Loads the BERT vocab file appended with word→id order preserved. */
        fun load(input: InputStream): WordPieceTokenizer {
            val vocab = LinkedHashMap<String, Int>()
            input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                vocab[line.trim()] = vocab.size
            }
            return WordPieceTokenizer(vocab)
        }
    }
}