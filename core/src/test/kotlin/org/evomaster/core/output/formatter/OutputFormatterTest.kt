package org.evomaster.core.output.formatter

import org.evomaster.core.output.formatter.MismatchedFormatException
import org.evomaster.core.output.formatter.OutputFormatter
import org.evomaster.core.search.gene.StringGene
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows


class OutputFormatterTest {

    @Test
    fun test(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val body = """
                {
                   "authorId": "VZyJz8z_Eu2",
                   "creationTime": "1921-3-13T10:18:56.000Z",
                   "newsId": "L"
                }
                """
        //should throw no exception
        OutputFormatter.JSON_FORMATTER.getFormatted(body)
    }


    @Test
    fun testMismatched(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val body = """

                   "authorId": "VZyJz8z_Eu2",
                   "creationTime": "1921-3-13T10:18:56.000Z",
                   "newsId": "L"
                }
                """

        assertThrows<MismatchedFormatException>{
            OutputFormatter.JSON_FORMATTER.getFormatted(body)
        }
    }
    @Test
    fun testEscapes(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1)
        val body = """
            {
            "name":"T\""
            }
        """

        val stringGene = StringGene("name", body)
        OutputFormatter.JSON_FORMATTER.getFormatted(body)
    }
}