package com.stabila.feature.keyboard.suggestion

/**
 * Utility object for parsing text before cursor into a [PredictionContext].
 */
object ContextExtractor {

    /**
     * Determines whether [ch] is considered part of a word.
     * Includes Unicode letters (English, Arabic, etc.), digits, and apostrophes (for contractions like "don't", "i'm").
     */
    fun isWordChar(ch: Char): Boolean {
        return ch.isLetterOrDigit() || ch == '\'' || ch == '’' || isArabicDiacritic(ch)
    }

    /**
     * Checks if [ch] is an Arabic haraka/diacritic or tatweel.
     */
    fun isArabicDiacritic(ch: Char): Boolean {
        return (ch in '\u064B'..'\u0652') || ch == '\u0640' || ch == '\u0670'
    }

    /**
     * Checks if [text] contains any Arabic characters.
     */
    fun isArabicText(text: String): Boolean {
        for (ch in text) {
            if (ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' || ch in '\u08A0'..'\u08FF' ||
                ch in '\uFB50'..'\uFDFF' || ch in '\uFE70'..'\uFEFF'
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Normalizes text by removing optional Arabic diacritics.
     */
    fun normalize(text: String, trimQuotes: Boolean = false): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (!isArabicDiacritic(ch)) {
                sb.append(ch)
            }
        }
        val lower = sb.toString().lowercase()
        return if (trimQuotes) lower.trim('\'', '’') else lower
    }

    /**
     * Extracts [PredictionContext] from the text immediately preceding the cursor.
     *
     * @param textBefore The text before the cursor (up to 200-500 characters).
     * @return [PredictionContext] containing normalized previous words and current partial word.
     */
    fun extract(textBefore: String): PredictionContext {
        if (textBefore.isEmpty()) {
            return PredictionContext()
        }

        var idx = textBefore.length - 1

        // Check if cursor is right at the end of a word being typed
        val currentWordBuilder = StringBuilder()
        while (idx >= 0 && isWordChar(textBefore[idx])) {
            currentWordBuilder.append(textBefore[idx])
            idx--
        }

        val currentWord = normalize(currentWordBuilder.reverse().toString(), trimQuotes = false)

        // Skip any non-word characters (spaces, punctuation, newlines) before previousWord
        while (idx >= 0 && !isWordChar(textBefore[idx])) {
            idx--
        }

        // Extract previousWord
        val prevWordBuilder = StringBuilder()
        while (idx >= 0 && isWordChar(textBefore[idx])) {
            prevWordBuilder.append(textBefore[idx])
            idx--
        }
        val previousWord = normalize(prevWordBuilder.reverse().toString(), trimQuotes = true)

        // Skip any non-word characters before previousWord2
        while (idx >= 0 && !isWordChar(textBefore[idx])) {
            idx--
        }

        // Extract previousWord2
        val prevWord2Builder = StringBuilder()
        while (idx >= 0 && isWordChar(textBefore[idx])) {
            prevWord2Builder.append(textBefore[idx])
            idx--
        }
        val previousWord2 = normalize(prevWord2Builder.reverse().toString(), trimQuotes = true)

        return PredictionContext(
            currentWord = currentWord,
            previousWord = previousWord,
            previousWord2 = previousWord2,
            isWordComplete = currentWord.isEmpty(),
        )
    }
}
