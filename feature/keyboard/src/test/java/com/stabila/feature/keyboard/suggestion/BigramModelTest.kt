package com.stabila.feature.keyboard.suggestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BigramModelTest {

    private lateinit var bigramModel: BigramModel

    private val sampleEnglishBigrams = listOf(
        "i\tam,want,have,think,will",
        "you\tare,can,have,will,know",
        "want\tto,a,you,the",
        "going\tto,home,back",
        "thank\tyou,god",
    )

    private val sampleArabicBigrams = listOf(
        "السلام\tعليكم,ورحمة",
        "صباح\tالخير,النور,الورد",
        "شكرا\tجزيلا,لك,لكم",
    )

    @Before
    fun setUp() {
        bigramModel = BigramModel()
    }

    @Test
    fun `isLoaded is false before load`() {
        assertFalse(bigramModel.isLoaded)
    }

    @Test
    fun `isLoaded is true after load`() {
        bigramModel.load(sampleEnglishBigrams, KeyboardLanguage.ENGLISH)
        assertTrue(bigramModel.isLoaded)
    }

    @Test
    fun `predictNext returns expected successor list in English`() {
        bigramModel.load(sampleEnglishBigrams, KeyboardLanguage.ENGLISH)
        val nextWords = bigramModel.predictNext("want", KeyboardLanguage.ENGLISH)
        assertEquals(listOf("to", "a", "you", "the"), nextWords)
    }

    @Test
    fun `predictNext returns expected successor list in Arabic`() {
        bigramModel.load(sampleArabicBigrams, KeyboardLanguage.ARABIC)
        val nextWords = bigramModel.predictNext("السلام", KeyboardLanguage.ARABIC)
        assertEquals(listOf("عليكم", "ورحمة"), nextWords)
    }

    @Test
    fun `predictNext is case insensitive`() {
        bigramModel.load(sampleEnglishBigrams, KeyboardLanguage.ENGLISH)
        val lower = bigramModel.predictNext("thank", KeyboardLanguage.ENGLISH)
        val upper = bigramModel.predictNext("THANK", KeyboardLanguage.ENGLISH)
        assertEquals(listOf("you", "god"), lower)
        assertEquals(lower, upper)
    }

    @Test
    fun `predictNext returns empty list for unknown word`() {
        bigramModel.load(sampleEnglishBigrams, KeyboardLanguage.ENGLISH)
        val nextWords = bigramModel.predictNext("unknown", KeyboardLanguage.ENGLISH)
        assertTrue(nextWords.isEmpty())
    }

    @Test
    fun `predictNext returns empty list for blank input`() {
        bigramModel.load(sampleEnglishBigrams, KeyboardLanguage.ENGLISH)
        assertTrue(bigramModel.predictNext("", KeyboardLanguage.ENGLISH).isEmpty())
        assertTrue(bigramModel.predictNext("   ", KeyboardLanguage.ENGLISH).isEmpty())
    }
}
