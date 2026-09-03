package com.stabila.feature.keyboard.suggestion

import java.util.concurrent.ConcurrentHashMap

/**
 * Contract for a word-suggestion and prediction engine.
 */
interface SuggestionEngine {

    companion object {
        const val MAX_SUGGESTIONS: Int = 3
    }

    /**
     * Whether the base dictionary index is loaded for English.
     */
    val isLoaded: Boolean

    /**
     * Whether the base dictionary index is loaded for the specified [language].
     */
    fun isLoaded(language: KeyboardLanguage): Boolean = isLoaded

    /**
     * Loads the unigram frequency wordlist into the engine for [language].
     */
    fun load(words: List<String>, language: KeyboardLanguage = KeyboardLanguage.ENGLISH)

    /**
     * Loads the bigram context model lines into the engine for [language].
     */
    fun loadBigrams(bigramLines: List<String>, language: KeyboardLanguage = KeyboardLanguage.ENGLISH) {}

    /**
     * Suggests completions or next words based on [context] and [language].
     */
    fun suggest(context: PredictionContext, language: KeyboardLanguage = KeyboardLanguage.ENGLISH): List<String>

    /**
     * Backward-compatible helper for prefix-only queries.
     */
    fun suggest(prefix: String, language: KeyboardLanguage = KeyboardLanguage.ENGLISH): List<String> =
        suggest(PredictionContext(currentWord = prefix), language)
}

/**
 * Frequency-ranked prefix Trie implementation of [SuggestionEngine] supporting multiple languages.
 */
class TrieSuggestionEngine : SuggestionEngine {

    private val roots = ConcurrentHashMap<KeyboardLanguage, TrieNode>().apply {
        put(KeyboardLanguage.ENGLISH, TrieNode())
        put(KeyboardLanguage.ARABIC, TrieNode())
    }

    private val loadedLanguages = ConcurrentHashMap.newKeySet<KeyboardLanguage>()

    override val isLoaded: Boolean
        get() = loadedLanguages.contains(KeyboardLanguage.ENGLISH)

    override fun isLoaded(language: KeyboardLanguage): Boolean {
        return loadedLanguages.contains(language)
    }

    override fun load(words: List<String>, language: KeyboardLanguage) {
        val newRoot = TrieNode()

        for (word in words) {
            val trimmed = word.trim()
            if (trimmed.length < 2) continue
            val lower = trimmed.lowercase()
            insertWord(newRoot, lower)
        }

        roots[language] = newRoot
        loadedLanguages.add(language)
    }

    private fun insertWord(root: TrieNode, word: String) {
        var node = root
        for (ch in word) {
            node = node.children.getOrPut(ch) { TrieNode() }
            if (node.topSuggestions.size < SuggestionEngine.MAX_SUGGESTIONS) {
                node.topSuggestions.add(word)
            }
        }
        node.isTerminal = true
    }

    override fun suggest(context: PredictionContext, language: KeyboardLanguage): List<String> {
        return suggest(context.currentWord, language)
    }

    override fun suggest(prefix: String, language: KeyboardLanguage): List<String> {
        if (!isLoaded(language) || prefix.isBlank()) return emptyList()

        val lower = prefix.lowercase()
        var node = roots[language] ?: return emptyList()

        for (ch in lower) {
            node = node.children[ch] ?: return emptyList()
        }

        return node.topSuggestions
    }
}
