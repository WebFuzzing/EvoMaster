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
        checkCanSample(RegexUtils.ignoreCaseRegex(s), listOf(s.uppercase(), s.lowercase()), 200)
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

    @Test
    fun testPosixCharacterClasses(){
        checkSameAsJava("""\p{Lower}\p{Upper}\p{ASCII}\p{Alpha}\p{Digit}\p{Alnum}\p{Punct}\p{Graph}
            |\p{Print}\p{Blank}\p{Cntrl}\p{XDigit}\p{Space}""".trimMargin())
    }

    @Test
    fun testUnicodeCategories(){
        val unicodeCategories = listOf(
            // Letters
            "Lu", "Ll", "GC=Lt", "general_category=Lm", "gc=Lo", "IsL",
            // Marks
            "Mn", "Mc", "Me", "M",
            // Numbers
            "Nd", "Nl", "No", "N",
            // Punctuation
            "Pc", "Pd", "Ps", "Pe", "Pi", "Pf", "Po", "P",
            // Symbols
            "Sm", "Sc", "Sk", "So", "S",
            // Separators
            "Zs", "Zl", "Zp", "Z",
            // Other
            "Cc", "Cf", "Cs", "Co", "Cn", "C"
        )
        for (label in unicodeCategories) {
            checkSameAsJava("\\p{$label}")
            checkSameAsJava("\\P{$label}")
        }
        checkSameAsJava("""Pe""")
    }

    @Test
    override fun testPredefinedCharClassInsideCharClass(){
        checkSameAsJava("""[\V\p{Lower}\p{Upper}\W\d]""")
        checkSameAsJava("""[a\p{Pe}]""")
        checkSameAsJava("""[\u00BB\u2019\u201D\u203A"'\p{Pe}\u0002¹²³]""")
        checkCanSample("""[a\p{Pe}b]""", ")", 1000)
    }

    @Test
    fun testPEscapesComplements(){
        checkSameAsJava("""\P{Lower}\P{Upper}\P{ASCII}\P{Alpha}\P{Digit}\P{Alnum}\P{Punct}\P{Graph}
            |\P{Print}\P{Blank}\P{Cntrl}\P{XDigit}\P{Space}""".trimMargin())
        checkSameAsJava("""\P{Pe}""")
    }

    @Test
    fun testUnicodeScripts(){
        val scriptLabels = listOf(
            "script=Arabic",
            "sc=Balinese",
            "IsLatin",
            "sc=greek",
            "SCRIPT=Yi",
        )
        for (label in scriptLabels) {
            checkSameAsJava("\\p{$label}")
            checkSameAsJava("\\P{$label}")
        }
    }

    @Test
    fun testUnicodeBlocks(){
        val blockLabels = listOf(
            "block=Oriya",
            "blk=Cyrillic",
            "InArabic",
            "blk=lao",
            "BLOCK=Bengali",
        )
        for (label in blockLabels) {
            checkSameAsJava("\\p{$label}")
            checkSameAsJava("\\P{$label}")
        }
    }

    @Test
    fun testUnicodeBinaryProperties(){
        val binaryProperyLabels = listOf(
            "Isalphabetic",
            "Isdigit",
            "Isideographic",
            "Isletter",
            "Islowercase",
            "Istitlecase",
            "Isuppercase",
            "Iswhite_space",
            "Ispunctuation",
            "Iscontrol",
            "Ishex_digit",
            "Isjoin_control",
            "Isnoncharacter_code_point",
            "IsNoncharacterCodePoint",
            "Isassigned",
        )
        for (label in binaryProperyLabels) {
            checkSameAsJava("\\p{$label}")
            checkSameAsJava("\\P{$label}")
        }
    }

    @Test
    fun testJavaCharacterMethods(){
        val javaCharacterMethodLabels = listOf(
            "javaDefined",
            "javaIdentifierIgnorable",
            "javaISOControl",
            "javaJavaIdentifierPart",
            "javaJavaIdentifierStart",
            "javaLetterOrDigit",
            "javaMirrored",
            "javaSpaceChar",
            "javaUnicodeIdentifierPart",
            "javaUnicodeIdentifierStart",
            "javaWhitespace",
            "javaAlphabetic",
            "javaDigit",
            "javaIdeographic",
            "javaLetter",
            "javaLowerCase",
            "javaTitleCase",
            "javaUpperCase",
        )
        for (label in javaCharacterMethodLabels) {
            checkSameAsJava("\\p{$label}")
            checkSameAsJava("\\P{$label}")
        }
    }

    @Test
    fun testFlags(){
        checkSameAsJava("""[(?iu)(?sd:x)]""")
        checkCanSample("""[(?iu)(?sd:x)]""", listOf("(", ")", "?", "i", "u", "s", "d", ":", "x"), 1000)
        checkSameAsJava("""(?i:)""")
        checkSameAsJava("""(?i:a.*[abc]+\w{1,3})""")
        checkCanSample("""(?i:a)(?i:A)""", listOf("aa", "aA", "Aa", "AA"), 100)
        checkSameAsJava("""(?i:\u00C2)""")
        checkSameAsJava("^((?i)@.+)$")
        checkSameAsJava("""(?iu:[\u03A1\u00C2]*)""")
        checkCanSample("""(?iu:\u03A1\u00C2)""", listOf("\u03a1\u00c2", "\u03a1\u00e2", "\u03c1\u00c2", "\u03c1\u00e2"), 100)
        checkSameAsJava("^((?iu)@.+)$")
        checkSameAsJava("^(?iu)")
        checkSameAsJava("(?iu)")
    }

    @Test
    fun testBackreferences(){
        checkSameAsJava("""(aaa)(?:bbb)\1""")
        checkSameAsJava("""(a|b|c)\1\1""")
        checkSameAsJava("""(?<randomName>a|b|c)\1\k<randomName>""")
        checkSameAsJava("""<>[(?<notAName>abc)]""")
        checkCanSample("""[(?<notAName>abc)]""", "N", 100)
        checkSameAsJava("""((A)(B(C)))\1\2\3\4""")
        checkSameAsJava("""(a)(b)(c)(d)(e)(f)(g)(h)(i)(j)(k)\10\11\12\120\120{3}""")
    }

    @Test
    override fun testJSExclusiveEscapes() {
        // JS exclusive
    }

    @Test
    fun testCharClassIntersectionSubtractionAndNesting(){
        checkSameAsJava("[abc-e[f-h]ij-l[m]n]")
        checkSameAsJava("[a&&a][a&&a&&a]")
        checkSameAsJava("[a-z&&[aeiou]]")
        checkSameAsJava("[a-z&&[^aeiou]]")
        checkSameAsJava("[a-z&&[a-p]&&[f-z]]")
        checkSameAsJava("[ac-e&&[a-d]]")
        checkSameAsJava("[\\w&&[a-z]]")
        checkSameAsJava("[a-z&&[b-y]]")
        checkSameAsJava("[a-z0-9&&[A-Z0-9]&&[2B4C]]")
        checkSameAsJava("[[a-c][x-z]&&[b-y]]")
        checkSameAsJava("[a-c&&[b-d]e-g]")
        checkSameAsJava("[^a-z&&[^aeiou]]")
        checkSameAsJava("[\\s&&[^\\n]]")
        checkSameAsJava("[a-c&&[c-e]]")
        checkSameAsJava("[a-z&&[a-z]]")
        checkSameAsJava("[a-ce-g&&[b-f]]")
        checkSameAsJava("[[a-z&&[a-p]]&&[f-z]]")
        checkSameAsJava("[a[b[c[d&&[\\w]]]][0-7&&\\d&&[0-5]&&1-5]]")
        checkSameAsJava("&&")
    }
}