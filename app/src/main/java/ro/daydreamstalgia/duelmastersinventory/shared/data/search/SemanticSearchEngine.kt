package ro.daydreamstalgia.duelmastersinventory.shared.data.search

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import ro.daydreamstalgia.duelmastersinventory.shared.utils.nlp.WordPieceTokenizer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * On-device sentence embedding via a bundled quantized MiniLM (all-MiniLM-L6-v2)
 * TFLite model, for semantic card search. The model graph already mean-pools its
 * token outputs into a single 384-dim sentence vector (see its Hugging Face
 * model card), so `embed` only has to tokenize, run inference and L2-normalize.
 *
 * The model's exported input tensors have generic signature names ("inputs",
 * "inputs_1") rather than "input_ids"/"attention_mask", and dynamic shape
 * (allocated as [1, 1] until resized) - so which slot is which, and the
 * sequence length, both have to be resolved at runtime rather than assumed.
 */
@Singleton
class SemanticSearchEngine @Inject constructor(@ApplicationContext context: Context) {

    private val tokenizer = WordPieceTokenizer(context, "$ASSET_DIR/vocab.txt")
    private val interpreter = Interpreter(
        loadModelFile(context),
        Interpreter.Options().setNumThreads(minOf(4, Runtime.getRuntime().availableProcessors()))
    )

    private val inputIdsIndex: Int
    private val attentionMaskIndex: Int
    private val embeddingDim: Int = interpreter.getOutputTensor(0).shape().last()

    init {
        inputIdsIndex = resolveInputIdsSlot()
        attentionMaskIndex = 1 - inputIdsIndex

        interpreter.resizeInput(inputIdsIndex, intArrayOf(1, SEQ_LEN))
        interpreter.resizeInput(attentionMaskIndex, intArrayOf(1, SEQ_LEN))
        interpreter.allocateTensors()

        Log.d(
            "SemanticSearchEngine",
            "Loaded model: inputIdsIndex=$inputIdsIndex seqLen=$SEQ_LEN embeddingDim=$embeddingDim"
        )
    }

    /**
     * The two input tensors are generically named, so name-matching can't tell
     * input_ids from attention_mask apart. Feed an out-of-vocab-range "poison"
     * id array into slot 0 - if that slot is really input_ids, the
     * embedding-lookup op faults on the invalid index; if it's attention_mask
     * (not used for indexing), it doesn't.
     */
    private fun resolveInputIdsSlot(): Int {
        val probeLen = 8
        interpreter.resizeInput(0, intArrayOf(1, probeLen))
        interpreter.resizeInput(1, intArrayOf(1, probeLen))
        interpreter.allocateTensors()

        val poison = IntArray(probeLen) { POISON_ID }
        val validMask = IntArray(probeLen) { if (it < 4) 1 else 0 }
        val inputs = arrayOf<Any>(poison.let { arrayOf(it) }, arrayOf(validMask))
        val output = arrayOf(FloatArray(embeddingDim))

        return try {
            interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to output))
            // No fault with poison in slot 0 -> slot 0 isn't used for embedding lookup.
            1
        } catch (e: Throwable) {
            Log.d("SemanticSearchEngine", "resolveInputIdsSlot: slot 0 faulted on poison ids (${e.message}) -> slot 0 is input_ids")
            0
        }
    }

    fun embed(text: String): FloatArray {
        val (inputIds, attentionMask) = tokenizer.tokenize(text, SEQ_LEN)

        val inputs = arrayOfNulls<Any>(2)
        inputs[inputIdsIndex] = arrayOf(inputIds)
        inputs[attentionMaskIndex] = arrayOf(attentionMask)

        val output = arrayOf(FloatArray(embeddingDim))
        interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to output))

        return l2Normalize(output[0])
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var normSq = 0f
        for (v in vector) normSq += v * v
        val norm = sqrt(normSq)
        if (norm == 0f) return vector
        return FloatArray(vector.size) { vector[it] / norm }
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("$ASSET_DIR/model.tflite")
        FileInputStream(assetFileDescriptor.fileDescriptor).use { input ->
            return input.channel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }

    companion object {
        private const val ASSET_DIR = "semantic_search"

        // Ability rules text is the only thing embedded (see DatabaseModule),
        // and some cards pack multiple clauses - 32 was cutting real ability
        // text off mid-sentence, which measurably hurt match quality/ranking.
        // 64 covers the large majority of card text without truncation.
        // Existing stored embeddings don't need re-seeding if this changes -
        // it only affects future embed() calls.
        private const val SEQ_LEN = 64

        // Well beyond the ~30k row bert-base-uncased vocab, so a Gather op
        // reading it as an embedding-table index is guaranteed to fault.
        private const val POISON_ID = 999_999

        /** Both vectors are assumed already L2-normalized (as `embed` returns), so this is a plain dot product. */
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            for (i in a.indices) dot += a[i] * b[i]
            return dot
        }
    }
}
