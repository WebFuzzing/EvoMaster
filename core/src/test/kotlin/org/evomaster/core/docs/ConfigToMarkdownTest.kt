package org.evomaster.core.docs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.Charset

class ConfigToMarkdownTest{

    @Test
    fun testUpdatedDocumentation(){

        /*
            If this test case fails, then you MUST
            re-run ConfigToMarkdown.main to generate the
            updated documentation file
         */

        val file = File("../docs/options.md")
        assertTrue(file.isFile)

        val text = file.readText(Charset.forName("utf-8")).replace("\r", "")
        val expected = ConfigToMarkdown.toMarkdown()

        assertEquals(expected, text)
    }
}