package com.stabila.feature.keyboard.suggestion

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ln

/**
 * Lightweight in-memory and local persistence layer for tracking user word choices and custom vocabulary.
 *
 * Privacy Guarantee:
 * - Operates 100% on-device.
 * - Stores only aggregated word counts and bigram counts.
 * - Never stores timestamps, sentences, full text logs, or sensitive input fields.
 */
class UserFrequencyStore {

    companion object {
        private const val MAX_TRACKED_WORDS = 2000
        private const val MAX_TRACKED_BIGRAMS = 3000
        private const val USER_LEARNED_WORD_THRESHOLD = 2
    }

    // Word -> frequency count
    private val wordCounts = ConcurrentHashMap<String, Int>()

    // PreviousWord -> (NextWord -> frequency count)
    private val bigramCounts = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    /**
     * Records a chosen or typed word in user history.
     */
    fun recordWord(word: String, previousWord: String = "") {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.length < 2) return

        // Update unigram count (capped for memory bounds)
        if (wordCounts.size < MAX_TRACKED_WORDS || wordCounts.containsKey(cleanWord)) {
            val count = wordCounts.getOrDefault(cleanWord, 0)
            wordCounts[cleanWord] = (count + 1).coerceAtMost(1000)
        }

        // Update bigram count
        val cleanPrev = previousWord.trim().lowercase()
        if (cleanPrev.isNotEmpty() && cleanPrev != cleanWord) {
            val innerMap = bigramCounts.computeIfAbsent(cleanPrev) { ConcurrentHashMap() }
            if (innerMap.size < 20) {
                val bCount = innerMap.getOrDefault(cleanWord, 0)
                innerMap[cleanWord] = (bCount + 1).coerceAtMost(1000)
            }
        }
    }

    /**
     * Computes the unigram frequency bonus score for [word].
     */
    fun getWordBonus(word: String): Double {
        val count = wordCounts[word.trim().lowercase()] ?: return 0.0
        return ln((count + 1).toDouble()) * 2.0
    }

    /**
     * Computes the bigram frequency bonus score for [previousWord] -> [nextWord].
     */
    fun getBigramBonus(previousWord: String, nextWord: String): Double {
        val cleanPrev = previousWord.trim().lowercase()
        val cleanNext = nextWord.trim().lowercase()
        if (cleanPrev.isEmpty() || cleanNext.isEmpty()) return 0.0

        val inner = bigramCounts[cleanPrev] ?: return 0.0
        val count = inner[cleanNext] ?: return 0.0
        return (count * 5.0).coerceAtMost(25.0)
    }

    /**
     * Returns custom user words matching [prefix] that have been typed at least [USER_LEARNED_WORD_THRESHOLD] times.
     */
    fun getLearnedWords(prefix: String): List<String> {
        if (prefix.isBlank()) return emptyList()
        val cleanPrefix = prefix.trim().lowercase()

        return wordCounts.entries
            .filter { (word, count) ->
                count >= USER_LEARNED_WORD_THRESHOLD && word.startsWith(cleanPrefix)
            }
            .sortedByDescending { it.value }
            .map { it.key }
            .take(SuggestionEngine.MAX_SUGGESTIONS)
    }

    /**
     * Returns top next-words learned for [previousWord].
     */
    fun getLearnedNextWords(previousWord: String): List<String> {
        val cleanPrev = previousWord.trim().lowercase()
        if (cleanPrev.isEmpty()) return emptyList()

        val inner = bigramCounts[cleanPrev] ?: return emptyList()
        return inner.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(SuggestionEngine.MAX_SUGGESTIONS)
    }

    /**
     * Exports current counts to a compact serialized string for storage.
     */
    fun exportToString(): String {
        val sb = StringBuilder()

        // Unigrams
        val unigrams = wordCounts.entries.joinToString(";") { "${it.key}:${it.value}" }
        sb.append("U=").append(unigrams).append("\n")

        // Bigrams
        val bigramEntries = mutableListOf<String>()
        for ((p, nextMap) in bigramCounts) {
            val pairs = nextMap.entries.joinToString(",") { "${it.key}:${it.value}" }
            if (pairs.isNotEmpty()) {
                bigramEntries.add("$p->$pairs")
            }
        }
        sb.append("B=").append(bigramEntries.joinToString(";"))

        return sb.toString()
    }

    /**
     * Restores counts from a serialized string.
     */
    fun loadFromString(data: String) {
        if (data.isBlank()) return

        for (line in data.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("U=")) {
                val payload = trimmed.substring(2)
                if (payload.isNotEmpty()) {
                    for (entry in payload.split(";")) {
                        val parts = entry.split(":")
                        if (parts.size == 2) {
                            val w = parts[0].trim().lowercase()
                            val c = parts[1].toIntOrNull() ?: 0
                            if (w.isNotEmpty() && c > 0) {
                                wordCounts[w] = c
                            }
                        }
                    }
                }
            } else if (trimmed.startsWith("B=")) {
                val payload = trimmed.substring(2)
                if (payload.isNotEmpty()) {
                    for (bEntry in payload.split(";")) {
                        val parts = bEntry.split("->")
                        if (parts.size == 2) {
                            val prev = parts[0].trim().lowercase()
                            val nextMap = ConcurrentHashMap<String, Int>()
                            for (pair in parts[1].split(",")) {
                                val pairParts = pair.split(":")
                                if (pairParts.size == 2) {
                                    val nxt = pairParts[0].trim().lowercase()
                                    val cnt = pairParts[1].toIntOrNull() ?: 0
                                    if (nxt.isNotEmpty() && cnt > 0) {
                                        nextMap[nxt] = cnt
                                    }
                                }
                            }
                            if (prev.isNotEmpty() && nextMap.isNotEmpty()) {
                                bigramCounts[prev] = nextMap
                            }
                        }
                    }
                }
            }
        }
    }
}
