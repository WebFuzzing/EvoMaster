package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.RegexGene
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 11-Sep-19.
 */
class GeneRegexJavaVisitorTest : GeneRegexEcma262VisitorTest() {

    override fun createGene(regex: String): RegexGene {
        return RegexHandler.createGeneForJVM(regex)
    }

    @Test
    fun testQuote(){
        checkSameAsJava("\\Qfoo.com\\E")
    }

    @Test
    fun testEmptyQuote(){
        checkSameAsJava("\\Q\\E")
    }

    @Test
    fun testBlankQuote(){
        checkSameAsJava("\\Q    \\E")
    }


    @Test
    fun testQuoteWholeLine(){
        checkSameAsJava("^(\\Qfoo.com\\E)$")
    }

    @Test
    fun testAssertionsAndQuotes(){
        checkSameAsJava("^((.*))$|^(\\Qfoo.com\\E)$")
    }

    @Test
    fun testQuotesInTheMiddle(){
        checkSameAsJava("^(\\d{4}-\\d{1,2}-\\d{1,2})\\Q-\\E(\\d+)$")
    }

    @Test
    fun testIssueQuotedE(){
        checkSameAsJava("\\Qfooebar\\E")
        checkSameAsJava("\\QfooEbar\\E")
    }

    @Test
    fun testIssueWithControlCharactersInIgnoreCase(){
        val s = "a[](){}\\\"^$.b"
        checkCanSample(RegexUtils.ignoreCaseRegex(s), listOf(s.toUpperCase(), s.toLowerCase()), 200)
    }

    @Test
    fun testCharEscapeRegex(){
        checkSameAsJava("\\s\\S\\d\\D\\w\\W")
    }

    @Test
    fun testIncreasingRange(){
        checkSameAsJava("[1-9]")
        checkSameAsJava("[ -!]")
    }

    @Test
    fun testDecreasingRange(){
        //checkSameAsJava("[!- ]") //not valid in Java
        //checkSameAsJava("[9-1]") //not valid in Java
        checkCanSample("[9-1]", listOf("1","5","9"),200)
    }

    @Test
    fun testJavaHexEscape(){
        checkSameAsJava("""x{3}\x{0}\x{FFFf}\x{0FFFf}\x{01FFFf}\x{10FFFf}""")
    }

    @Test
    fun testJavaOctalEscape(){
        checkSameAsJava("""00\00\07\077\0377\0378\0400""")
    }

    @Test
    override fun testControlEscape(){
        checkSameAsJava("""aefnrt\a\e\f\n\r\t""")
    }

    @Test
    override fun testControlLetterEscape() {
        checkSameAsJava("""cac!\ca\cg\cz\cA\cG\cZ\c@\c[\c\\c]\c^\c\c_\c?""")
    }

    @Test
    fun testJavaCharClassEscape(){
        checkSameAsJava("""\v\V\h\H""")
    }
}