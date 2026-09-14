package com.example.movietime.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    @Test
    fun stringList_nullHandling() {
        assertNull(converters.fromStringList(null))
        assertNull(converters.toStringList(null))
    }

    @Test
    fun stringList_emptyListHandling() {
        val emptyList = emptyList<String>()
        val json = converters.fromStringList(emptyList)
        val result = converters.toStringList(json)

        assertNotNull(json)
        assertEquals(emptyList, result)
    }

    @Test
    fun stringList_roundTripConversion() {
        val originalList = listOf("Action", "Drama", "Sci-Fi")
        val json = converters.fromStringList(originalList)
        val result = converters.toStringList(json)

        assertEquals(originalList, result)
    }

    @Test
    fun intList_nullHandling() {
        assertNull(converters.fromIntList(null))
        assertNull(converters.toIntList(null))
    }

    @Test
    fun intList_emptyListHandling() {
        val emptyList = emptyList<Int>()
        val json = converters.fromIntList(emptyList)
        val result = converters.toIntList(json)

        assertEquals(emptyList, result)
    }

    @Test
    fun intList_roundTripConversion() {
        val originalList = listOf(28, 12, 878, 10759)
        val json = converters.fromIntList(originalList)
        val result = converters.toIntList(json)

        assertEquals(originalList, result)
    }

    private fun assertNotNull(value: Any?) {
        org.junit.Assert.assertNotNull(value)
    }
}