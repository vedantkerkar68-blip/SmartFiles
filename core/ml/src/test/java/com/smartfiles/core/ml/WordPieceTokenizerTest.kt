package com.smartfiles.core.ml

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test

class WordPieceTokenizerTest {

    private val vocab = buildMap<String, Int> {
        put("[PAD]", 0)
        put("[CLS]", 101)
        put("[SEP]", 102)
        put("[UNK]", 100)
        put("hello", 7592)
        put("world", 2088)
        put("machine", 2600)
        put("learning", 1550)
        put("##ing", 1089)
        put("the", 1996)
        put("weather", 3565)
        put("sunn", 2827)
        put("##y", 2243)
    }

    private val tokenizer = WordPieceTokenizer(vocab, maxSeq = 8)

    @Test
    fun tokenIds_padsToCapacityAndAddsClsSep() {
        val ids = tokenizer.tokenIds("hello world")
        assertThat(ids.size).isEqualTo(8)
        assertThat(ids[0]).isEqualTo(vocab.getValue("[CLS]"))
        assertThat(ids[1]).isEqualTo(vocab.getValue("hello"))
        assertThat(ids[2]).isEqualTo(vocab.getValue("world"))
        assertThat(ids[3]).isEqualTo(vocab.getValue("[SEP]"))
        assertThat(ids[4]).isEqualTo(0) // [PAD]
        assertThat(ids[7]).isEqualTo(0)
    }

    @Test
    fun tokenIds_wordPieceSplitsUnknownWord() {
        // "learning" -> "learn##ing" is not shown explicitly; here we split "sunny".
        val ids = tokenizer.tokenIds("the sunny weather")
        assertThat(ids[1]).isEqualTo(vocab.getValue("the"))
        assertThat(ids[2]).isEqualTo(vocab.getValue("sunn"))
        assertThat(ids[3]).isEqualTo(vocab.getValue("##y"))
        assertThat(ids[4]).isEqualTo(vocab.getValue("weather"))
        assertThat(ids[5]).isEqualTo(vocab.getValue("[SEP]"))
    }

    @Test
    fun tokenIds_fallsBackToUnkForGarbage() {
        val ids = tokenizer.tokenIds("zzzzqqqqxxxx")
        assertThat(ids[1]).isEqualTo(vocab.getValue("[UNK]"))
    }

    @Test
    fun tokenIds_truncatesAtCapacity() {
        val ids = tokenizer.tokenIds("hello world hello world hello world")
        assertThat(ids.size).isEqualTo(8)
        assertThat(ids.last()).isEqualTo(vocab.getValue("[SEP]")) // SEP kept, rest dropped
    }

    @Test
    fun attentionMask_marksPadding() {
        val mask = tokenizer.attentionMask("hello world")
        assertThat(mask.size).isEqualTo(8)
        assertThat(mask.take(4)).isEqualTo(listOf(1, 1, 1, 1))
        assertThat(mask.drop(4)).isEqualTo(listOf(0, 0, 0, 0))
    }

    @Test
    fun normalizedWords_lowercasesAndStripsAccents() {
        // Lowercasing + NFD accent strip: "Café" -> "cafe".
        val ids = tokenizer.tokenIds("Café")
        // "cafe" is unknown here so falls to [UNK]; validates no crash + UNK handling.
        assertThat(ids[1]).isEqualTo(vocab.getValue("[UNK]"))
    }

    @Test
    fun load_preservesLineOrderIds() {
        val bytes = ("[PAD]\n[UNK]\n[CLS]\nhello\n").toByteArray(Charsets.UTF_8)
        val t = WordPieceTokenizer.load(ByteArrayInputStream(bytes))
        assertThat(t.tokenIds("hello")).hasLength(WordPieceTokenizer.MAX_SEQ)
        assertThat(t.tokenIds("hello")[1]).isEqualTo(3)
    }
}