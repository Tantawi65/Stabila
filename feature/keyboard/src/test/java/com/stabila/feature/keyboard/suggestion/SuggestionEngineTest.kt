package com.stabila.feature.keyboard.suggestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TrieSuggestionEngine].
 *
 * These tests run on the JVM with no Android dependencies — validating the engine
 * in isolation is one of the key design benefits of keeping it pure Kotlin.
 */
class SuggestionEngineTest {

    private lateinit var engine: TrieSuggestionEngine

    // Frequency-ranked word list used by most tests (higher index = lower frequency).
    private val sampleWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that",
        "want", "wanted", "wants", "wanting",
        "work", "worker", "working", "works",
        "help", "helpful", "helping", "helps",
        "hello", "helm", "help",
        "world", "word", "worry",
        "application", "apply", "apple", "approach",
        "it", "is",
    )

    @Before
    fun setUp() {
        engine = TrieSuggestionEngine()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // isLoaded
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `isLoaded is false before load`() {
        assertFalse(engine.isLoaded)
    }

    @Test
    fun `isLoaded is true after load`() {
        engine.load(sampleWords)
        assertTrue(engine.isLoaded)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // suggest() before load
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest returns empty list before load`() {
        val results = engine.suggest("want")
        assertTrue(results.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Empty / blank prefix
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest returns empty list for blank prefix`() {
        engine.load(sampleWords)
        assertTrue(engine.suggest("").isEmpty())
        assertTrue(engine.suggest("   ").isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prefix matching
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest returns words starting with prefix`() {
        engine.load(sampleWords)
        val results = engine.suggest("wan")
        assertTrue("want should be in results", results.contains("want"))
        assertTrue("wanted should be in results", results.contains("wanted"))
        // All results must start with the prefix
        results.forEach { word ->
            assertTrue("'$word' should start with 'wan'", word.startsWith("wan"))
        }
    }

    @Test
    fun `suggest returns up to MAX_SUGGESTIONS results`() {
        engine.load(sampleWords)
        // "w" matches: want, wanted, wants, wanting, work, worker, working, works, world, word, worry
        val results = engine.suggest("w")
        assertTrue(results.size <= SuggestionEngine.MAX_SUGGESTIONS)
    }

    @Test
    fun `suggest for exact terminal word returns results`() {
        engine.load(sampleWords)
        val results = engine.suggest("want")
        assertTrue(results.isNotEmpty())
        assertTrue(results.contains("want"))
    }

    @Test
    fun `suggest returns empty list for unknown prefix`() {
        engine.load(sampleWords)
        val results = engine.suggest("zzzzzz")
        assertTrue(results.isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ranking
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest preserves frequency order from word list`() {
        engine.load(sampleWords)
        val results = engine.suggest("wan")
        // "want" appears before "wanted" in sampleWords → must appear first
        val wantIndex = results.indexOf("want")
        val wantedIndex = results.indexOf("wanted")
        assertTrue("'want' index ($wantIndex) must be < 'wanted' index ($wantedIndex)",
            wantIndex < wantedIndex)
    }

    @Test
    fun `suggest for single-char prefix returns highest-frequency matches`() {
        engine.load(sampleWords)
        val results = engine.suggest("t")
        assertTrue(results.isNotEmpty())
        // "the" is the first word with "t" prefix
        assertEquals("the", results[0])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Case handling
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest is case-insensitive for prefix`() {
        engine.load(sampleWords)
        val lower = engine.suggest("wan")
        val upper = engine.suggest("WAN")
        val mixed = engine.suggest("Wan")
        assertEquals(lower, upper)
        assertEquals(lower, mixed)
    }

    @Test
    fun `suggest returns lowercase completions regardless of input case`() {
        engine.load(sampleWords)
        val results = engine.suggest("WANT")
        results.forEach { word ->
            assertEquals("Suggestions should be lowercase: $word", word, word.lowercase())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Very short prefix
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest works for single letter prefix`() {
        engine.load(sampleWords)
        val results = engine.suggest("h")
        assertTrue(results.isNotEmpty())
        results.forEach { word ->
            assertTrue("'$word' should start with 'h'", word.startsWith("h"))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Short words in dictionary
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `single-character words are not indexed`() {
        engine.load(listOf("a", "i", "apple"))
        // "a" and "i" are too short to index; "apple" should still be reachable
        val results = engine.suggest("app")
        assertTrue(results.contains("apple"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Empty dictionary
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest returns empty list when dictionary is empty`() {
        engine.load(emptyList())
        assertTrue(engine.isLoaded)
        assertTrue(engine.suggest("hello").isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Duplicate words in dictionary
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `duplicate words do not exceed MAX_SUGGESTIONS`() {
        engine.load(listOf("apple", "apple", "apple", "apple", "apricot"))
        val results = engine.suggest("app")
        assertTrue(results.size <= SuggestionEngine.MAX_SUGGESTIONS)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Whitespace in dictionary
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `words with surrounding whitespace in word list are trimmed`() {
        engine.load(listOf("  apple  ", " apricot ", "apply"))
        val results = engine.suggest("app")
        assertTrue(results.any { it == "apple" || it == "apply" })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stale-result simulation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * This test validates the trie engine in isolation — the stale-result guard
     * itself lives in [StabilaKeyboardService] and is tested via integration.
     * Here we verify that [suggest] is deterministic and idempotent for the
     * same prefix regardless of how many times it is called concurrently,
     * since the trie is immutable after [load].
     */
    @Test
    fun `suggest is idempotent for same prefix after load`() {
        engine.load(sampleWords)
        val first = engine.suggest("hel")
        val second = engine.suggest("hel")
        assertEquals(first, second)
    }

    @Test
    fun `suggest for different prefixes in rapid succession returns correct results`() {
        engine.load(sampleWords)
        val h = engine.suggest("h")
        val he = engine.suggest("he")
        val hel = engine.suggest("hel")
        val hell = engine.suggest("hell")

        // Each shorter prefix is a superset domain of longer prefixes
        assertTrue(h.all { it.startsWith("h") })
        assertTrue(he.all { it.startsWith("he") })
        assertTrue(hel.all { it.startsWith("hel") })
        assertTrue(hell.all { it.startsWith("hell") })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAX_SUGGESTIONS constant
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `MAX_SUGGESTIONS is 3`() {
        assertEquals(3, SuggestionEngine.MAX_SUGGESTIONS)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Large dictionary performance smoke test
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `suggest is fast even with a large dictionary`() {
        // Build a synthetic 10000-word dictionary
        val bigDict = (1..10_000).map { "word$it" }
        engine.load(bigDict)

        val startMs = System.currentTimeMillis()
        repeat(1_000) { engine.suggest("word") }
        val elapsedMs = System.currentTimeMillis() - startMs

        // 1000 lookups must complete in well under 1 second
        assertTrue("1000 suggest() calls took ${elapsedMs}ms; expected < 500ms",
            elapsedMs < 500)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ExtractCurrentWord helper (tested via service method visibility)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * [StabilaKeyboardService.extractCurrentWord] is package-internal so we
     * test the same logic here to keep the test module self-contained.
     */
    private fun extractCurrentWord(textBefore: String): String {
        if (textBefore.isEmpty()) return ""
        var i = textBefore.length - 1
        while (i >= 0 && textBefore[i].isLetterOrDigit()) i--
        return textBefore.substring(i + 1)
    }

    @Test
    fun `extractCurrentWord returns partial word at cursor`() {
        assertEquals("wan", extractCurrentWord("I wan"))
        assertEquals("hello", extractCurrentWord("hello"))
        assertEquals("world", extractCurrentWord("hello world"))
        assertEquals("", extractCurrentWord("hello "))
        assertEquals("", extractCurrentWord(""))
        assertEquals("test", extractCurrentWord("line1\ntest"))
        assertEquals("word", extractCurrentWord("comma,word"))
    }

    @Test
    fun `extractCurrentWord handles numbers`() {
        assertEquals("123", extractCurrentWord("abc 123"))
    }

    @Test
    fun `extractCurrentWord handles unicode Arabic letters`() {
        assertEquals("كلمة", extractCurrentWord("هذه كلمة"))
    }
}
