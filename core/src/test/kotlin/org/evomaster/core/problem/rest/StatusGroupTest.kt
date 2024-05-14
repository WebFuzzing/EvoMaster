package org.evomaster.core.problem.rest

import org.junit.Assert
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Test cases for StatusGroup
 */
class StatusGroupTest {

    /**
     * All possible status groups
     */
    private val statusGroup1xx = StatusGroup.G_1xx
    private val statusGroup2xx = StatusGroup.G_2xx
    private val statusGroup3xx = StatusGroup.G_3xx
    private val statusGroup4xx = StatusGroup.G_4xx
    private val statusGroup5xx = StatusGroup.G_5xx

    @Test
    fun isInGroupWith1xx() {

        // for each number between 100 and 199 inclusive, they are in group
        for( i in 100..199) {
            Assertions.assertTrue(statusGroup1xx.isInGroup(i))
        }

        // 99 and 200 are not in the group (edge cases)
        Assertions.assertFalse(statusGroup1xx.isInGroup(99))
        Assertions.assertFalse(statusGroup1xx.isInGroup(200))

        // 2000 is not in the group 2xx
        Assertions.assertFalse(statusGroup1xx.isInGroup(2000))

        // -2000 is not in the group 2xx
        Assertions.assertFalse(statusGroup1xx.isInGroup(-2000))
    }


    @Test
    fun isInGroupWith2xx() {

        // for each number between 200 and 299 inclusive, they are in group
        for( i in 200..299) {
            Assertions.assertTrue(statusGroup2xx.isInGroup(i))
        }

        // 199 and 300 are not in the group (edge cases)
        Assertions.assertFalse(statusGroup2xx.isInGroup(199))
        Assertions.assertFalse(statusGroup2xx.isInGroup(300))

        // 3000 is not in the group 2xx
        Assertions.assertFalse(statusGroup2xx.isInGroup(3000))

        // -3000 is not in the group 2xx
        Assertions.assertFalse(statusGroup2xx.isInGroup(-3000))
    }

    @Test
    fun isInGroupWith3xx() {

        // for each number between 300 and 399 inclusive, they are in group
        for( i in 300..399) {
            Assertions.assertTrue(statusGroup3xx.isInGroup(i))
        }

        // 299 and 400 are not in the group (edge cases)
        Assertions.assertFalse(statusGroup3xx.isInGroup(299))
        Assertions.assertFalse(statusGroup3xx.isInGroup(400))

        // 4000 is not in the group 3xx
        Assertions.assertFalse(statusGroup3xx.isInGroup(4000))

        // -4000 is not in the group 3xx
        Assertions.assertFalse(statusGroup3xx.isInGroup(-4000))
    }

    @Test
    fun isInGroupWith4xx() {

        // for each number between 300 and 399 inclusive, they are in group
        for( i in 400..499) {
            Assertions.assertTrue(statusGroup4xx.isInGroup(i))
        }

        // 399 and 500 are not in the group (edge cases)
        Assertions.assertFalse(statusGroup4xx.isInGroup(399))
        Assertions.assertFalse(statusGroup4xx.isInGroup(500))

        // 5000 is not in the group 3xx
        Assertions.assertFalse(statusGroup4xx.isInGroup(5000))

        // -5000 is not in the group 3xx
        Assertions.assertFalse(statusGroup4xx.isInGroup(-5000))
    }

    @Test
    fun isInGroupWith5xx() {

        // for each number between 300 and 399 inclusive, they are in group
        for( i in 500..599) {
            Assertions.assertTrue(statusGroup5xx.isInGroup(i))
        }

        // 499 and 600 are not in the group (edge cases)
        Assertions.assertFalse(statusGroup5xx.isInGroup(499))
        Assertions.assertFalse(statusGroup5xx.isInGroup(600))

        // 6000 is not in the group 3xx
        Assertions.assertFalse(statusGroup5xx.isInGroup(6000))

        // -6000 is not in the group 3xx
        Assertions.assertFalse(statusGroup5xx.isInGroup(-6000))
    }

    /**
     * null element should not be in any of groups 1xx, 2xx, 3xx, 4xx, 5xx
     */
    @Test
    fun testIsInGroupWithNull(){

        Assertions.assertFalse(statusGroup1xx.isInGroup(null))
        Assertions.assertFalse(statusGroup2xx.isInGroup(null))
        Assertions.assertFalse(statusGroup3xx.isInGroup(null))
        Assertions.assertFalse(statusGroup4xx.isInGroup(null))
        Assertions.assertFalse(statusGroup5xx.isInGroup(null))

    }

    // Testing exceptional cases.
    @Test
    fun isInGroupParameterRetrievedFromParse() {
        Assertions.assertTrue(statusGroup1xx.isInGroup(Integer.parseInt("101")))
    }

    @Test
    fun isInGroupParameterRetrievedFromParseDouble() {
        Assert.assertThrows(Exception::class.java) { statusGroup1xx.isInGroup(Integer.parseInt("101.89")) }
    }

    @Test
    fun isInGroupParameterRetrievedFromParseString() {
        Assert.assertThrows(Exception::class.java) { statusGroup1xx.isInGroup(Integer.parseInt("abcdef")) }
    }
}