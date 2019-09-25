package org.evomaster.core.search.gene

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateGeneTest {

    @Test
    fun testDefaultFormat() {
        val gene = DateGene("dateTime")
        gene.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        assertEquals(
                "1978-07-31",
                gene.getValueAsRawString()
        )
    }

    @Test
    fun testISOLocalDateFormat() {
        val gene = DateGene("dateTime",
                dateGeneFormat = DateGene.DateGeneFormat.ISO_LOCAL_DATE_FORMAT)
        gene.apply {
            year.value = 1978
            month.value = 7
            day.value = 31
        }
        assertEquals(
                "1978-07-31",
                gene.getValueAsRawString()
        )
    }

}