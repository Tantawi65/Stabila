package com.stabila.feature.keyboard.suggestion

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hybrid prediction engine combining:
 * 1. Prefix-matching Trie (for fast prefix completions)
 * 2. Bigram context model (for next-word prediction)
 * 3. UserFrequencyStore (for personalized on-device ranking and learning)
 *
 * Supports both English and Arabic language modes seamlessly.
 */
@Singleton
class HybridSuggestionEngine @Inject constructor(
    private val trieEngine: TrieSuggestionEngine,
    val bigramModel: BigramModel,
    val userFrequencyStore: UserFrequencyStore,
) : SuggestionEngine {

    companion object {
        private val DEFAULT_FALLBACK_EN = listOf("the", "to", "and", "i", "a", "you", "it", "in")
        private val DEFAULT_FALLBACK_AR = listOf("في", "من", "على", "إلى", "هذا", "شكرا", "السلام", "كل")
    }

    override val isLoaded: Boolean
        get() = trieEngine.isLoaded

    override fun isLoaded(language: KeyboardLanguage): Boolean {
        return trieEngine.isLoaded(language)
    }

    override fun load(words: List<String>, language: KeyboardLanguage) {
        trieEngine.load(words, language)
    }

    override fun loadBigrams(bigramLines: List<String>, language: KeyboardLanguage) {
        bigramModel.load(bigramLines, language)
    }

    override fun suggest(context: PredictionContext, language: KeyboardLanguage): List<String> {
        // Resolve effective language based on keyboard mode or auto-detection from text
        val effectiveLang = if (language == KeyboardLanguage.ARABIC ||
            ContextExtractor.isArabicText(context.currentWord) ||
            ContextExtractor.isArabicText(context.previousWord)
        ) {
            KeyboardLanguage.ARABIC
        } else {
            KeyboardLanguage.ENGLISH
        }

        if (!trieEngine.isLoaded(effectiveLang)) {
            // If the specific language isn't loaded yet, try fallback if loaded
            if (!trieEngine.isLoaded) return emptyList()
        }

        return if (context.isWordComplete || context.currentWord.isEmpty()) {
            predictNextWords(context.previousWord, effectiveLang)
        } else {
            completeCurrentWord(context.currentWord, context.previousWord, effectiveLang)
        }
    }

    /**
     * Completes an in-progress partial word using Trie, User Learning, and Bigram context.
     */
    private fun completeCurrentWord(
        currentWord: String,
        previousWord: String,
        language: KeyboardLanguage,
    ): List<String> {
        val cleanPrefix = currentWord.trim().lowercase()
        if (cleanPrefix.isEmpty()) return emptyList()

        val candidateScores = LinkedHashMap<String, Double>()

        // 1. Trie prefix matches
        val trieMatches = trieEngine.suggest(cleanPrefix, language)
        trieMatches.forEachIndexed { index, word ->
            val rankScore = (SuggestionEngine.MAX_SUGGESTIONS - index) * 10.0
            candidateScores[word] = rankScore
        }

        // 2. User-learned custom words matching prefix
        val userLearned = userFrequencyStore.getLearnedWords(cleanPrefix)
        userLearned.forEach { word ->
            val existing = candidateScores.getOrDefault(word, 0.0)
            candidateScores[word] = existing + 15.0
        }

        // 3. Bigram candidates matching current prefix
        if (previousWord.isNotEmpty()) {
            val bigramCandidates = bigramModel.predictNext(previousWord, language)
            bigramCandidates.filter { it.startsWith(cleanPrefix) }.forEachIndexed { idx, word ->
                val bigramBonus = (5.0 - idx).coerceAtLeast(1.0) * 4.0
                val existing = candidateScores.getOrDefault(word, 0.0)
                candidateScores[word] = existing + bigramBonus
            }
        }

        // 4. Apply personal frequency bonuses
        for (word in candidateScores.keys.toList()) {
            val unigramBonus = userFrequencyStore.getWordBonus(word)
            val bigramBonus = if (previousWord.isNotEmpty()) {
                userFrequencyStore.getBigramBonus(previousWord, word)
            } else 0.0
            candidateScores[word] = candidateScores.getValue(word) + unigramBonus + bigramBonus
        }

        return candidateScores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(SuggestionEngine.MAX_SUGGESTIONS)
    }

    /**
     * Predicts likely next words when the user completed a word or typed a space.
     */
    private fun predictNextWords(
        previousWord: String,
        language: KeyboardLanguage,
    ): List<String> {
        val cleanPrev = previousWord.trim().lowercase()
        val fallbacks = if (language == KeyboardLanguage.ARABIC) DEFAULT_FALLBACK_AR else DEFAULT_FALLBACK_EN

        if (cleanPrev.isEmpty()) {
            return fallbacks.take(SuggestionEngine.MAX_SUGGESTIONS)
        }

        val candidateScores = LinkedHashMap<String, Double>()

        // 1. User learned bigrams for this exact previous word
        val learnedNext = userFrequencyStore.getLearnedNextWords(cleanPrev)
        learnedNext.forEachIndexed { index, word ->
            candidateScores[word] = 50.0 - (index * 5.0)
        }

        // 2. Base Bigram dataset candidates
        val bigramMatches = bigramModel.predictNext(cleanPrev, language)
        bigramMatches.forEachIndexed { index, word ->
            val baseScore = (bigramMatches.size - index) * 6.0
            val existing = candidateScores.getOrDefault(word, 0.0)
            candidateScores[word] = existing + baseScore
        }

        // 3. Apply user bonuses
        for (word in candidateScores.keys.toList()) {
            val bigramBonus = userFrequencyStore.getBigramBonus(cleanPrev, word)
            val unigramBonus = userFrequencyStore.getWordBonus(word)
            candidateScores[word] = candidateScores.getValue(word) + bigramBonus + unigramBonus
        }

        // 4. Fallback if fewer than MAX_SUGGESTIONS found
        if (candidateScores.size < SuggestionEngine.MAX_SUGGESTIONS) {
            for (fallback in fallbacks) {
                if (fallback != cleanPrev && !candidateScores.containsKey(fallback)) {
                    candidateScores[fallback] = 1.0
                }
                if (candidateScores.size >= SuggestionEngine.MAX_SUGGESTIONS) break
            }
        }

        return candidateScores.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(SuggestionEngine.MAX_SUGGESTIONS)
    }
}
