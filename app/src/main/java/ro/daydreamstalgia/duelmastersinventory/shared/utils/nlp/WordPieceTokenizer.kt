package ro.daydreamstalgia.duelmastersinventory.shared.utils.nlp

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Standard BERT tokenization (basic whitespace/punctuation split, lowercase,
 * then greedy longest-match WordPiece against a bert-base-uncased-style vocab.txt),
 * ported from HuggingFace's reference implementation so the model's own vocab
 * asset drives tokenization with no native/JNI dependency.
 */
class WordPieceTokenizer(context: Context, vocabAssetPath: String) {

    private val vocab: Map<String, Int>

    init {
        val map = HashMap<String, Int>()
        BufferedReader(InputStreamReader(context.assets.open(vocabAssetPath))).use { reader ->
            var index = 0
            reader.forEachLine { line ->
                if (line.isNotEmpty()) {
                    map[line] = index
                    index++
                }
            }
        }
        vocab = map
    }

    private val clsId = vocab[CLS_TOKEN] ?: error("Vocab missing $CLS_TOKEN")
    private val sepId = vocab[SEP_TOKEN] ?: error("Vocab missing $SEP_TOKEN")
    private val unkId = vocab[UNK_TOKEN] ?: error("Vocab missing $UNK_TOKEN")
    private val padId = vocab[PAD_TOKEN] ?: 0

    /**
     * Returns (inputIds, attentionMask), both of length [maxLen], padded with [PAD_TOKEN].
     */
    fun tokenize(text: String, maxLen: Int): Pair<IntArray, IntArray> {
        val wordPieces = basicTokenize(text).flatMap { wordPieceTokenize(it) }

        val ids = ArrayList<Int>(maxLen)
        ids.add(clsId)
        for (piece in wordPieces) {
            if (ids.size >= maxLen - 1) break
            ids.add(vocab[piece] ?: unkId)
        }
        ids.add(sepId)

        val inputIds = IntArray(maxLen) { padId }
        val attentionMask = IntArray(maxLen)
        for (i in ids.indices) {
            inputIds[i] = ids[i]
            attentionMask[i] = 1
        }

        return inputIds to attentionMask
    }

    private fun basicTokenize(text: String): List<String> {
        val lower = text.lowercase()
        val tokens = ArrayList<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.clear()
            }
        }

        for (ch in lower) {
            when {
                ch.isWhitespace() -> flush()
                isPunctuation(ch) || isCjk(ch) -> {
                    flush()
                    tokens.add(ch.toString())
                }
                else -> current.append(ch)
            }
        }
        flush()

        return tokens
    }

    private fun wordPieceTokenize(word: String): List<String> {
        if (word.length > MAX_INPUT_CHARS_PER_WORD) return listOf(UNK_TOKEN)

        val pieces = ArrayList<String>()
        var start = 0
        while (start < word.length) {
            var end = word.length
            var found: String? = null
            while (start < end) {
                var candidate = word.substring(start, end)
                if (start > 0) candidate = "##$candidate"
                if (vocab.containsKey(candidate)) {
                    found = candidate
                    break
                }
                end--
            }
            if (found == null) return listOf(UNK_TOKEN)
            pieces.add(found)
            start = end
        }
        return pieces
    }

    private fun isPunctuation(ch: Char): Boolean {
        val code = ch.code
        if ((code in 33..47) || (code in 58..64) || (code in 91..96) || (code in 123..126)) return true
        return Character.getType(ch).let {
            it == Character.CONNECTOR_PUNCTUATION.toInt() ||
                it == Character.DASH_PUNCTUATION.toInt() ||
                it == Character.START_PUNCTUATION.toInt() ||
                it == Character.END_PUNCTUATION.toInt() ||
                it == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
                it == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
                it == Character.OTHER_PUNCTUATION.toInt() ||
                it == Character.MATH_SYMBOL.toInt()
        }
    }

    private fun isCjk(ch: Char): Boolean {
        val code = ch.code
        return (code in 0x4E00..0x9FFF) || (code in 0x3400..0x4DBF) ||
            (code in 0xF900..0xFAFF) || (code in 0x3040..0x30FF)
    }

    companion object {
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
        private const val UNK_TOKEN = "[UNK]"
        private const val PAD_TOKEN = "[PAD]"
        private const val MAX_INPUT_CHARS_PER_WORD = 200
    }
}
