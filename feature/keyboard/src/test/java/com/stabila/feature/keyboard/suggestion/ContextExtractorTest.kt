package com.stabila.feature.keyboard.suggestion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextExtractorTest {

    @Test
    fun `extract empty string returns empty context`() {
        val context = ContextExtractor.extract("")
        assertEquals("", context.currentWord)
        assertEquals("", context.previousWord)
        assertEquals("", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    @Test
    fun `extract partial word currently typed`() {
        val context = ContextExtractor.extract("I wan")
        assertEquals("wan", context.currentWord)
        assertEquals("i", context.previousWord)
        assertEquals("", context.previousWord2)
        assertFalse(context.isWordComplete)
    }

    @Test
    fun `extract completed word with trailing space`() {
        val context = ContextExtractor.extract("I want ")
        assertEquals("", context.currentWord)
        assertEquals("want", context.previousWord)
        assertEquals("i", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    @Test
    fun `extract handles multiple words and three-word context`() {
        val context = ContextExtractor.extract("I really want to ")
        assertEquals("", context.currentWord)
        assertEquals("to", context.previousWord)
        assertEquals("want", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    @Test
    fun `extract handles contractions with apostrophes`() {
        val context = ContextExtractor.extract("I don't ")
        assertEquals("", context.currentWord)
        assertEquals("don't", context.previousWord)
        assertEquals("i", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    @Test
    fun `extract handles typing contraction prefix`() {
        val context = ContextExtractor.extract("I don'")
        assertEquals("don'", context.currentWord)
        assertEquals("i", context.previousWord)
        assertFalse(context.isWordComplete)
    }

    @Test
    fun `extract handles punctuation boundaries`() {
        val context = ContextExtractor.extract("Hello, world! How ")
        assertEquals("", context.currentWord)
        assertEquals("how", context.previousWord)
        assertEquals("world", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    @Test
    fun `extract handles newlines`() {
        val context = ContextExtractor.extract("First line\nsecond ")
        assertEquals("", context.currentWord)
        assertEquals("second", context.previousWord)
        assertEquals("line", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Arabic Context Extraction Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `extract Arabic partial word being typed`() {
        val context = ContextExtractor.extract("السلام علي")
        assertEquals("علي", context.currentWord)
        assertEquals("السلام", context.previousWord)
        assertFalse(context.isWordComplete)
    }

    @Test
    fun `extract Arabic completed word with trailing space`() {
        val context = ContextExtractor.extract("السلام عليكم ")
        assertEquals("", context.currentWord)
        assertEquals("عليكم", context.previousWord)
        assertEquals("السلام", context.previousWord2)
        assertTrue(context.isWordComplete)
    }

    @Test
    fun `isArabicText accurately detects Arabic characters`() {
        assertTrue(ContextExtractor.isArabicText("مرحبا"))
        assertTrue(ContextExtractor.isArabicText("Hello مرحبا"))
        assertFalse(ContextExtractor.isArabicText("Hello world!"))
    }
}
