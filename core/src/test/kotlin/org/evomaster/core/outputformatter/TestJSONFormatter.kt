package org.evomaster.core.outputformatter

import org.evomaster.core.output.formatter.OutputFormatter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue


class TestJSONFormatter {

    @Test
    fun test(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1 ?:false)
        var body = "{" +
                "\"authorId\":\"VZyJz8z_Eu2\", " +
                "\"creationTime\":\"1921-3-13T10:18:56.000Z\", " +
                "\"newsId\":\"L\"" +
                "}";
        print(OutputFormatter.JSON_FORMATTER.getFormatted(body))
    }


    @Test
    fun testMismatched(){
        assertTrue(OutputFormatter.getFormatters()?.size == 1 ?:false)
        var body =
                "\"authorId\":\"VZyJz8z_Eu2\", " +
                "\"creationTime\":\"1921-3-13T10:18:56.000Z\", " +
                "\"newsId\":\"L\"" +
                "}";
        print(OutputFormatter.JSON_FORMATTER.getFormatted(body))
    }
}