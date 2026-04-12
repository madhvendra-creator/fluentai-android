package com.supereva.fluentai.domain.util

enum class DiffOperation {
    KEEP, INSERT, DELETE
}

data class WordDiff(
    val word: String,
    val operation: DiffOperation
)

object WordDiffUtil {

    /**
     * Computes a word-level diff between [original] and [corrected] strings
     * using a simple Longest Common Subsequence (LCS) approach.
     */
    fun computeDiff(original: String, corrected: String): List<WordDiff> {
        // Split by whitespace but keep punctuation attached to words for simplicity,
        // or split by regex to cleanly separate punctuation.
        // For a basic inline diff, splitting by space is a good start.
        val originalWords = original.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val correctedWords = corrected.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

        if (originalWords.isEmpty() && correctedWords.isEmpty()) return emptyList()
        if (originalWords.isEmpty()) return correctedWords.map { WordDiff(it, DiffOperation.INSERT) }
        if (correctedWords.isEmpty()) return originalWords.map { WordDiff(it, DiffOperation.DELETE) }

        // Find LCS matrix
        val m = originalWords.size
        val n = correctedWords.size
        val lcs = Array(m + 1) { IntArray(n + 1) }

        for (i in 1..m) {
            for (j in 1..n) {
                if (originalWords[i - 1] == correctedWords[j - 1]) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1
                } else {
                    lcs[i][j] = maxOf(lcs[i - 1][j], lcs[i][j - 1])
                }
            }
        }

        // Backtrack to find differences
        var i = m
        var j = n
        val diffs = mutableListOf<WordDiff>()

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && originalWords[i - 1] == correctedWords[j - 1]) {
                diffs.add(0, WordDiff(originalWords[i - 1], DiffOperation.KEEP))
                i--
                j--
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                diffs.add(0, WordDiff(correctedWords[j - 1], DiffOperation.INSERT))
                j--
            } else if (i > 0 && (j == 0 || lcs[i][j - 1] < lcs[i - 1][j])) {
                diffs.add(0, WordDiff(originalWords[i - 1], DiffOperation.DELETE))
                i--
            }
        }

        return diffs
    }
}
