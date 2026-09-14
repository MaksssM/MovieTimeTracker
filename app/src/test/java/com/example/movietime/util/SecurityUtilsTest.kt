package com.example.movietime.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityUtilsTest {

    @Test
    fun sanitizeInput_escapesHtmlSpecialCharacters() {
        val input = "<script>alert('xss & \"test\"')</script>"
        val sanitized = SecurityUtils.sanitizeInput(input)

        assertEquals("&lt;script&gt;alert(&#x27;xss &amp; &quot;test&quot;&#x27;)&lt;/script&gt;", sanitized)
    }

    @Test
    fun sanitizeInput_trimsWhitespace() {
        val input = "   hello world   "
        val sanitized = SecurityUtils.sanitizeInput(input)

        assertEquals("hello world", sanitized)
    }

    @Test
    fun sanitizeInput_plainStringUnchanged() {
        val input = "Inception (2010)"
        val sanitized = SecurityUtils.sanitizeInput(input)

        assertEquals("Inception (2010)", sanitized)
    }

    @Test
    fun clearCharArray_overwritesWithNullChar() {
        val password = charArrayOf('s', 'e', 'c', 'r', 'e', 't')
        SecurityUtils.clearCharArray(password)

        val expected = charArrayOf('\u0000', '\u0000', '\u0000', '\u0000', '\u0000', '\u0000')
        assertArrayEquals(expected, password)
    }
}