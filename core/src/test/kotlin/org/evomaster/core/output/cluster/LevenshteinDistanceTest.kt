package org.evomaster.core.output.cluster

import org.evomaster.core.output.clustering.metrics.LevenshteinDistance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LevenshteinDistanceTest {
    /**
     * Tests for Levenshtein Distance
     */

    @Test
    fun levenshteinDistanceTestIdentical(){
        val s1 = "aaaaa"

        assertEquals(0.0, LevenshteinDistance.distance(s1, s1))
    }

    @Test
    fun levenshteinDistanceTestMax(){
        val s1 = "aaaaa"
        val s2 = "zzzzz"

        assertEquals(1.0, LevenshteinDistance.distance(s1, s2))
    }

    @Test
    fun levenshteinDistanceTestMed(){
        val s1 = "aaaaaa"
        val s2 = "zzzaaa"

        assertEquals(0.5, LevenshteinDistance.distance(s1, s2))
    }
}