package org.evomaster.e2etests.spring.graphql.errors

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

object ErrorsInStatisticsUtil {


    fun checkErrorsInStatistics(path: String, num: Int, numLine: Int, numFaults: Int, numNoError: Int, numActions: Int){
        val file = File(path)
        assertTrue(file.exists())
        val stats = file.readLines()
        val header = stats.first().split(",")
        val results = stats[1].split(",")

        assertEquals(2, stats.size)

        assertEquals(numActions, results[header.indexOf("distinctActions")].toInt())

        val actual = results[header.indexOf("gqlErrors")].toInt()
        assertEquals(num, actual)

        val actualfaults = results[header.indexOf("potentialFaults")].toInt()
        assertEquals(numFaults, actualfaults)

        /*
            no error might not be controlled.
         */
        if (numNoError != -1){
            assertEquals(numNoError, results[header.indexOf("gqlNoErrors")].toInt())
        }
    }

}