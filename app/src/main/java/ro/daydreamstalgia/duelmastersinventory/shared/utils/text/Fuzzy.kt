package ro.daydreamstalgia.duelmastersinventory.shared.utils.text

import kotlin.math.max

/**
 * Normalized Levenshtein similarity in [0, 1] (1 = identical), case-insensitive.
 * Catches single-word typos (e.g. "Nastasha" vs "Natasha") that a semantic
 * embedding alone isn't reliable for, since a short proper noun typo doesn't
 * reliably land near the correct spelling in embedding space.
 */
fun fuzzySimilarity(a: String, b: String): Float {
    val s1 = a.lowercase()
    val s2 = b.lowercase()
    if (s1.isEmpty() && s2.isEmpty()) return 1f
    val maxLen = max(s1.length, s2.length)
    if (maxLen == 0) return 1f

    val distance = levenshteinDistance(s1, s2)
    return 1f - (distance.toFloat() / maxLen)
}

private fun levenshteinDistance(s1: String, s2: String): Int {
    val prev = IntArray(s2.length + 1) { it }
    val curr = IntArray(s2.length + 1)

    for (i in 1..s1.length) {
        curr[0] = i
        for (j in 1..s2.length) {
            curr[j] = if (s1[i - 1] == s2[j - 1]) {
                prev[j - 1]
            } else {
                1 + minOf(prev[j - 1], prev[j], curr[j - 1])
            }
        }
        for (j in 0..s2.length) prev[j] = curr[j]
    }

    return prev[s2.length]
}
