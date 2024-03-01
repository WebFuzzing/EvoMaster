package org.evomaster.core.search.gene.sql

import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.datetime.DateGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SqlMultiRangeGeneTest {

    @Test
    fun testEmptyMultirange() {
        val multirangeGene = SqlMultiRangeGene("multiint4rangegene",
                template = SqlRangeGene("int4range",
                        template = IntegerGene("int4")
                ))

        assertEquals("\"{}\"", multirangeGene.getValueAsPrintableString())
    }

    @Test
    fun testNonEmptyIntMultirange() {
        val multirangeGene = SqlMultiRangeGene("multiint4rangegene",
                template = SqlRangeGene("int4range",
                        template = IntegerGene("int4")
                ))

        val rangeGene = multirangeGene.template.copy() as SqlRangeGene<IntegerGene>
        multirangeGene.rangeGenes.addElement(rangeGene)

        assertEquals("\"{[ 0 , 0 ]}\"", multirangeGene.getValueAsPrintableString())
    }

    @Test
    fun testManyIntRangesMultirange() {
        val multirangeGene = SqlMultiRangeGene("multiint4rangegene",
                template = SqlRangeGene("int4range",
                        template = IntegerGene("int4")
                ))

        val rangeGene0 = multirangeGene.template.copy() as SqlRangeGene<IntegerGene>
        multirangeGene.rangeGenes.addElement(rangeGene0)

        val rangeGene1 = multirangeGene.template.copy() as SqlRangeGene<IntegerGene>
        multirangeGene.rangeGenes.addElement(rangeGene1)

        assertEquals("\"{[ 0 , 0 ], [ 0 , 0 ]}\"", multirangeGene.getValueAsPrintableString())
    }

    @Test
    fun testManyDateRangesMultirange() {
        val multirangeGene = SqlMultiRangeGene("multirange",
                template = SqlRangeGene("range",
                        template = DateGene("date")))

        val rangeGene0 = multirangeGene.template.copy() as SqlRangeGene<DateGene>
        multirangeGene.rangeGenes.addElement(rangeGene0)

        val rangeGene1 = multirangeGene.template.copy() as SqlRangeGene<DateGene>
        multirangeGene.rangeGenes.addElement(rangeGene1)

        assertEquals("\"{[ 2016-03-12 , 2016-03-12 ], [ 2016-03-12 , 2016-03-12 ]}\"", multirangeGene.getValueAsPrintableString())
    }

}