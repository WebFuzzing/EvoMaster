package org.evomaster.core.problem.rest.statistics

import bar.examples.it.spring.body.BodyController
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.rest.IntegrationTestRestBase
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.Statistics
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class StatisticsIntegrationTest : IntegrationTestRestBase(){

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            //the actual problem does not matter, as long as we can build a valid injector context
            initClass(BodyController())
        }
    }

    @Test
    fun testHeaderCount(){

        val statistics = injector.getInstance(Statistics::class.java)
        val solution = Solution<RestIndividual>(mutableListOf(), "", "", Termination.NONE, listOf(), listOf())
        val lines = statistics.getHeadersAndElementsCSVLines(solution)
        val csv = "${lines.first}\n${lines.second}\n"

        val data = CSVParser.parse(csv, CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get())

        assertTrue(data.headerNames.contains(Statistics.DISTINCT_ACTIONS), "Headers: ${data.headerNames}")

        val records = data.records
        assertEquals(1, records.size)
        val entry = records[0]

        assertEquals(data.headerNames.size, entry.size())
    }
}