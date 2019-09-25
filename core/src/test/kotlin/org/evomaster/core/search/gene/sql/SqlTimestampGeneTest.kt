package org.evomaster.core.search.gene.sql

import org.evomaster.core.database.DbActionGeneBuilder
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SqlTimestampGeneTest {

    private val DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"

    @Test
    fun testValidLocalDateTime() {
        val gene = DbActionGeneBuilder().buildSqlTimestampGene("timestamp")
        val rawString = gene.getValueAsRawString()
        val formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)
        LocalDateTime.parse(rawString, formatter)
    }

    @Test
    fun testValidDate() {
        val gene = DbActionGeneBuilder().buildSqlTimestampGene("timestamp")
        val rawString = gene.date.getValueAsRawString()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        LocalDate.parse(rawString, formatter)
    }

    @Test
    fun testValidDateAfterManyRandomizations() {

        val randomness = Randomness()

        val gene = DbActionGeneBuilder().buildSqlTimestampGene("timestamp")

        for (i in 1..10000) {
            gene.randomize(randomness, forceNewValue = true)
            val rawString = gene.getValueAsRawString()
            val formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)
            LocalDateTime.parse(rawString, formatter)
        }

    }


}