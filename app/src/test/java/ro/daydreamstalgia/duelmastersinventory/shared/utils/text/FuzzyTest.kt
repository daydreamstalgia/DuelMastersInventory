package ro.daydreamstalgia.duelmastersinventory.shared.utils.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyTest {

    @Test
    fun identicalStrings_scoreOne() {
        assertEquals(1f, fuzzySimilarity("Natasha", "Natasha"), 0.001f)
    }

    @Test
    fun caseInsensitive() {
        assertEquals(1f, fuzzySimilarity("NATASHA", "natasha"), 0.001f)
    }

    @Test
    fun singleCharacterTypo_scoresHigh() {
        // "Nastasha" vs "Natasha": one transposition-ish edit, should stay close to 1.
        val score = fuzzySimilarity("Nastasha", "Natasha")
        assertTrue("expected high similarity for a single-typo name, got $score", score >= 0.75f)
    }

    @Test
    fun unrelatedStrings_scoreLow() {
        val score = fuzzySimilarity("Natasha", "Bolshack Dragon")
        assertTrue("expected low similarity for unrelated names, got $score", score < 0.3f)
    }

    @Test
    fun emptyStrings_scoreOne() {
        assertEquals(1f, fuzzySimilarity("", ""), 0.001f)
    }
}
