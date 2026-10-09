package ro.ddnostalgia.duelmastersinventory.shared.utils.nlp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WordPieceTokenizerTest {

    private lateinit var tokenizer: WordPieceTokenizer

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tokenizer = WordPieceTokenizer(context, "semantic_search/vocab.txt")
    }

    @Test
    fun tokenize_padsAndMasksToRequestedLength() {
        val (inputIds, attentionMask) = tokenizer.tokenize("put shield in hand", 16)

        assertEquals(16, inputIds.size)
        assertEquals(16, attentionMask.size)

        // [CLS] + 4 words (all in-vocab, no wordpiece splitting expected) + [SEP] = 6 real tokens.
        assertEquals(6, attentionMask.sum())
        for (i in 6 until 16) {
            assertEquals(0, attentionMask[i])
            assertEquals(0, inputIds[i]) // [PAD] id
        }
    }

    @Test
    fun tokenize_splitsOutOfVocabWordIntoWordpieces() {
        // "Bolshack" isn't a whole-word vocab entry - should fall back to
        // subword pieces (##-prefixed continuations) rather than a single [UNK].
        val (inputIds, attentionMask) = tokenizer.tokenize("Bolshack", 16)
        val realTokenCount = attentionMask.sum()

        // [CLS] + at least 2 wordpieces + [SEP]
        assertTrue("expected multiple wordpieces for an unknown word, got $realTokenCount", realTokenCount >= 4)
        assertTrue(inputIds.toList().distinct().size > 1)
    }

    @Test
    fun tokenize_truncatesToMaxLen() {
        val longText = (1..50).joinToString(" ") { "word$it" }
        val (inputIds, attentionMask) = tokenizer.tokenize(longText, 8)

        assertEquals(8, inputIds.size)
        assertEquals(8, attentionMask.sum()) // fully packed, no room for padding
    }
}
