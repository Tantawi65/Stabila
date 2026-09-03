package com.stabila.feature.keyboard.suggestion

/**
 * Encapsulates the text context around the cursor used for prediction.
 *
 * @property currentWord The partial word currently being typed at the cursor (empty if cursor is after whitespace).
 * @property previousWord The word immediately preceding [currentWord], normalized to lowercase (empty if none).
 * @property previousWord2 The word preceding [previousWord], normalized to lowercase (empty if none).
 * @property isWordComplete True when the cursor is positioned immediately after whitespace or a separator,
 *                          indicating the user finished a word and is awaiting next-word suggestions.
 */
data class PredictionContext(
    val currentWord: String = "",
    val previousWord: String = "",
    val previousWord2: String = "",
    val isWordComplete: Boolean = currentWord.isEmpty(),
)
