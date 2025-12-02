package org.evomaster.core.problem.security

import org.evomaster.core.problem.security.service.SSRFAnalyser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class URLNameTest {

    private val fileName = "src/test/resources/security/names.csv"

    private fun loadURLNames(): List<Triple<String, String, Boolean>> {
        val output = mutableListOf<Triple<String, String, Boolean>>()
        val file = File(fileName)
        if (!file.exists()) {
            throw Exception("File does not exist")
        }

        try {
            file.bufferedReader().use { reader ->
                // We ignore the handling of CSV header since it's a custom file
                reader.readLines().forEach { line ->
                    val values = line.split(",")
                    if (values.size != 3) {
                        throw Exception("Wrong number of values")
                    }
                    output.add(Triple(values[0], values[1], values[2].toInt() == 1))
                }
            }
        } catch (e: Exception) {
            throw Exception("Error reading CSV file: ${e.message}")
        }

        return output
    }


    @Test
    fun testURLNames() {
        val sa = SSRFAnalyser()
        val urlNames = loadURLNames()

        urlNames.forEach { v->
            assertEquals(v.third, sa.manualClassifier(v.first, v.second))
        }
    }

}
