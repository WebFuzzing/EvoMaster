package org.evomaster.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TimeUtilsTest {

    @Test
    fun testRetryAfter(){

        verifyIsNegative("")
        verifyIsNegative("-4")
        verifyIsNegative("2.3")
        verifyIsNegative("tomorrow")
        verifyIsNegative("2126-05-11T14:30:45Z")
        verifyIsNegative("2126-05-11 14:30:45")

        //this is a valid string, but in the past
        verifyIsNegative("Fri, 31 Dec 1999 23:59:59 GMT")


        verifyIsPositive("42")
        verifyIsPositive("Sat, 06 Nov 2094 08:49:37 GMT")
        verifyIsPositive("Sun, 31 Dec 2084 23:59:59 GMT")

        //Java actually check if day of the week is correct...
        verifyIsNegative("Mon, 31 Dec 2084 23:59:59 GMT")
    }

    private fun verifyIsNegative(retryAfter: String){
        val x = TimeUtils.getTimeToWaitInSeconds(retryAfter)
        assertTrue(x < 0, "Non-negative value for '$retryAfter': $x")
    }

    private fun verifyIsPositive(retryAfter: String){
        val x = TimeUtils.getTimeToWaitInSeconds(retryAfter)
        assertTrue(x >= 0, "Non-positive value for '$retryAfter': $x")
    }

}