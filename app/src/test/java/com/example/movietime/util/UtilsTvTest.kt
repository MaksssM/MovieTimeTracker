package com.example.movietime.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTvTest {

    @Test
    fun computeTotalRuntimeForTv_nullEpisodeRuntime() {
        val total = Utils.computeTotalRuntimeForTv(null, 5)
        assertEquals(0, total)
    }

    @Test
    fun computeTotalRuntimeForTv_negativeEpisodes() {
        val total = Utils.computeTotalRuntimeForTv(30, -3)
        assertEquals(0, total)
    }

    @Test
    fun computeTotalRuntimeForTv_normal() {
        val total = Utils.computeTotalRuntimeForTv(45, 10)
        assertEquals(450, total)
    }
}

