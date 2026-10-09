package ro.ddnostalgia.duelmastersinventory.shared.data.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class SemanticSearchEngineTest {

    private lateinit var engine: SemanticSearchEngine

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        engine = SemanticSearchEngine(context)
    }

    @Test
    fun embed_returnsUnitLengthVectorOfExpectedDimension() {
        val vector = engine.embed("put a shield into your hand")

        assertEquals(384, vector.size)

        var normSq = 0f
        for (v in vector) normSq += v * v
        assertEquals(1f, sqrt(normSq), 0.01f)
    }

    @Test
    fun embed_semanticallySimilarSentencesScoreHigherThanUnrelatedOnes() {
        val query = engine.embed("put shield in hand")
        val paraphrase = engine.embed("put a shield card into your hand")
        val unrelated = engine.embed("destroy one of your opponent's creatures")

        val similarScore = SemanticSearchEngine.cosineSimilarity(query, paraphrase)
        val unrelatedScore = SemanticSearchEngine.cosineSimilarity(query, unrelated)

        assertTrue(
            "expected paraphrase ($similarScore) to score higher than unrelated text ($unrelatedScore)",
            similarScore > unrelatedScore
        )
    }

    @Test
    fun embed_isDeterministic() {
        val a = engine.embed("Bolshack Dragon")
        val b = engine.embed("Bolshack Dragon")

        assertEquals(1f, SemanticSearchEngine.cosineSimilarity(a, b), 0.001f)
    }
}
