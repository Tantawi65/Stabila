package com.stabila.feature.keyboard.suggestion

import java.util.concurrent.ConcurrentHashMap

/**
 * Compact on-device bigram prediction model supporting both English and Arabic.
 * Maps a preceding word to a pre-ranked list of likely successor words.
 */
class BigramModel {

    private val bigramMaps = ConcurrentHashMap<KeyboardLanguage, Map<String, List<String>>>().apply {
        put(KeyboardLanguage.ENGLISH, emptyMap())
        put(KeyboardLanguage.ARABIC, emptyMap())
    }

    private val loadedLanguages = ConcurrentHashMap.newKeySet<KeyboardLanguage>()

    val isLoaded: Boolean
        get() = loadedLanguages.contains(KeyboardLanguage.ENGLISH)

    fun isLoaded(language: KeyboardLanguage): Boolean {
        return loadedLanguages.contains(language)
    }

    /**
     * Loads bigram entries from lines of format: `<word>\t<successor1>,<successor2>,...`
     */
    fun load(lines: List<String>, language: KeyboardLanguage = KeyboardLanguage.ENGLISH) {
        val map = HashMap<String, List<String>>(lines.size)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || !trimmed.contains('\t')) continue

            val parts = trimmed.split('\t', limit = 2)
            if (parts.size == 2) {
                val word = parts[0].trim().lowercase()
                val successors = parts[1].split(',')
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() && it != word }

                if (word.isNotEmpty() && successors.isNotEmpty()) {
                    map[word] = successors
                }
            }
        }

        bigramMaps[language] = map
        loadedLanguages.add(language)
    }

    /**
     * Predicts the most likely next words given [previousWord] and [language].
     */
    fun predictNext(previousWord: String, language: KeyboardLanguage = KeyboardLanguage.ENGLISH): List<String> {
        if (previousWord.isBlank()) return emptyList()
        val key = previousWord.trim().lowercase()
        val map = bigramMaps[language] ?: return emptyList()
        return map[key] ?: emptyList()
    }
}
