package com.stabila.feature.keyboard.suggestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HybridEngineTest {

    private lateinit var trieEngine: TrieSuggestionEngine
    private lateinit var bigramModel: BigramModel
    private lateinit var userStore: UserFrequencyStore
    private lateinit var hybridEngine: HybridSuggestionEngine

    private val sampleEnglishWords = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "want", "wanted", "wants", "wanting",
        "thank", "thanks", "thankful",
        "going", "go", "goes", "gone",
        "work", "working", "worker", "works",
        "apple", "apply", "application"
    )

    private val sampleEnglishBigrams = listOf(
        "i\tam,have,want,will,think",
        "you\tare,can,have,will,know",
        "want\tto,a,the,you",
        "going\tto,home,back,out",
        "thank\tyou,god,so",
        "how\tare,is,do,did",
    )

    private val sampleArabicWords = listOf(
        "في", "من", "على", "إلى", "هذا", "هذه", "ذلك", "كل", "أنا", "أنت",
        "السلام", "عليكم", "ورحمة", "وبركاته",
        "شكرا", "جزيلا", "عفوا",
        "صباح", "الخير", "مساء", "النور",
        "أريد", "المساعدة", "الذهاب", "العمل",
        "تطبيق", "تطوير", "تطبيقنا"
    )

    private val sampleArabicBigrams = listOf(
        "السلام\tعليكم,ورحمة,وبركاته",
        "صباح\tالخير,النور,الورد",
        "مساء\tالخير,النور,الورد",
        "شكرا\tجزيلا,لك,لكم",
        "أريد\tأن,المساعدة,الذهاب",
    )

    @Before
    fun setUp() {
        trieEngine = TrieSuggestionEngine()
        bigramModel = BigramModel()
        userStore = UserFrequencyStore()
        hybridEngine = HybridSuggestionEngine(trieEngine, bigramModel, userStore)

        hybridEngine.load(sampleEnglishWords, KeyboardLanguage.ENGLISH)
        hybridEngine.loadBigrams(sampleEnglishBigrams, KeyboardLanguage.ENGLISH)

        hybridEngine.load(sampleArabicWords, KeyboardLanguage.ARABIC)
        hybridEngine.loadBigrams(sampleArabicBigrams, KeyboardLanguage.ARABIC)
    }

    @Test
    fun `hybrid engine is loaded after loading words`() {
        assertTrue(hybridEngine.isLoaded)
        assertTrue(hybridEngine.isLoaded(KeyboardLanguage.ENGLISH))
        assertTrue(hybridEngine.isLoaded(KeyboardLanguage.ARABIC))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // English Prefix & Next-Word Prediction
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `predicts English prefix completions when typing partial word`() {
        val context = ContextExtractor.extract("I wan")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ENGLISH)

        assertTrue(results.isNotEmpty())
        assertEquals("want", results[0])
        assertTrue(results.all { it.startsWith("wan") })
    }

    @Test
    fun `predicts English next words after I want with trailing space`() {
        val context = ContextExtractor.extract("I want ")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ENGLISH)

        assertTrue(results.isNotEmpty())
        assertEquals("to", results[0])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Arabic Prefix & Next-Word Prediction
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `predicts Arabic prefix completions when typing partial Arabic word`() {
        val context = ContextExtractor.extract("صب")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ARABIC)

        assertTrue(results.isNotEmpty())
        assertEquals("صباح", results[0])
    }

    @Test
    fun `predicts Arabic next words after السلام with trailing space`() {
        val context = ContextExtractor.extract("السلام ")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ARABIC)

        assertTrue(results.isNotEmpty())
        assertEquals("عليكم", results[0])
    }

    @Test
    fun `predicts Arabic next words after صباح with trailing space`() {
        val context = ContextExtractor.extract("صباح ")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ARABIC)

        assertTrue(results.isNotEmpty())
        assertEquals("الخير", results[0])
    }

    @Test
    fun `predicts Arabic next words after شكرا with trailing space`() {
        val context = ContextExtractor.extract("شكرا ")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ARABIC)

        assertTrue(results.isNotEmpty())
        assertEquals("جزيلا", results[0])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Language Switch / Auto-Detection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `auto-detects Arabic from context even if language param was English`() {
        val context = ContextExtractor.extract("صباح ")
        val results = hybridEngine.suggest(context, KeyboardLanguage.ENGLISH)

        assertTrue(results.isNotEmpty())
        assertEquals("الخير", results[0])
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User frequency learning
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `user chosen word climbs in ranking`() {
        val context = ContextExtractor.extract("app")
        val initial = hybridEngine.suggest(context, KeyboardLanguage.ENGLISH)
        assertEquals("apple", initial[0])

        repeat(10) {
            userStore.recordWord("apply")
        }

        val updated = hybridEngine.suggest(context, KeyboardLanguage.ENGLISH)
        assertEquals("apply", updated[0])
    }

    @Test
    fun `learns new unknown Arabic word typed multiple times`() {
        val customArabicWord = "تطبيقناالجديد"

        val initial = hybridEngine.suggest(ContextExtractor.extract("تطبيقناال"), KeyboardLanguage.ARABIC)
        assertFalse(initial.contains(customArabicWord))

        repeat(3) {
            userStore.recordWord(customArabicWord)
        }

        val updated = hybridEngine.suggest(ContextExtractor.extract("تطبيقناال"), KeyboardLanguage.ARABIC)
        assertTrue(updated.contains(customArabicWord))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Performance test
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `hybrid suggest is fast for both languages under 1ms per query`() {
        val enPrefix = ContextExtractor.extract("I wan")
        val enNext = ContextExtractor.extract("I want ")
        val arPrefix = ContextExtractor.extract("صب")
        val arNext = ContextExtractor.extract("السلام ")

        val start = System.currentTimeMillis()
        repeat(500) {
            hybridEngine.suggest(enPrefix, KeyboardLanguage.ENGLISH)
            hybridEngine.suggest(enNext, KeyboardLanguage.ENGLISH)
            hybridEngine.suggest(arPrefix, KeyboardLanguage.ARABIC)
            hybridEngine.suggest(arNext, KeyboardLanguage.ARABIC)
        }
        val elapsed = System.currentTimeMillis() - start

        assertTrue("2000 hybrid multi-language queries took ${elapsed}ms; expected < 500ms", elapsed < 500)
    }
}
