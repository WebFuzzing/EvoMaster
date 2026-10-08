package org.evomaster.core.problem.rest.builder

import org.evomaster.core.problem.util.ParamUtil
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.datetime.DateTimeGene
import org.evomaster.core.search.gene.datetime.TimeGene
import org.evomaster.core.search.gene.network.InetGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.numeric.BigIntegerGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.regex.RegexGene
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigInteger


class AdvancedFormatsGeneTest {

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    /**
     * Build a single-property DTO whose property "x" has the given [type] and [format],
     * and return the (unwrapped) gene created for that property.
     */
    private fun buildFormatGene(type: String, format: String): Gene {
        val name = "Dto_${format.replace('-', '_')}"
        val dtoSchema = """
            "$name": {
                "type": "object",
                "properties": {
                    "x": {"type": "$type", "format": "$format"}
                },
                "required": ["x"]
            }
        """.trimIndent()

        val obj = RestActionBuilderV3.createGeneForDTO(
            name, dtoSchema, null, RestActionBuilderV3.Options(enableAdvancedFormats = true)
        ) as ObjectGene

        val field = obj.fields.find { it.name == "x" }!!
        return ParamUtil.getValueGene(field)
    }

    private fun assertAlwaysInRange(gene: Gene, min: BigInteger, max: BigInteger) {
        val rand = Randomness()
        if (!gene.initialized) gene.doInitialize(rand)
        for (i in 0..2000) {
            gene.randomize(rand, false)
            val v = BigInteger(gene.getValueAsRawString())
            assertTrue(v in min..max, "Generated out-of-range value $v for [$min, $max]")
        }
    }

    /**
     * Valid boundary values must be accepted; values just outside the range must be rejected.
     * Note: [org.evomaster.core.search.gene.numeric.NumberGene] does not range-check BigInteger
     * bounds, so this is only applied to Int/Long-backed formats.
     */
    private fun assertBoundaries(gene: SimpleGene, min: BigInteger, max: BigInteger) {
        val rand = Randomness()
        if (!gene.initialized) gene.doInitialize(rand)

        gene.setValueWithRawString(min.toString())
        assertTrue(gene.isLocallyValid(), "min boundary $min should be valid")
        gene.setValueWithRawString(max.toString())
        assertTrue(gene.isLocallyValid(), "max boundary $max should be valid")

        gene.setValueWithRawString((min - BigInteger.ONE).toString())
        assertFalse(gene.isLocallyValid(), "value below min (${min - BigInteger.ONE}) should be invalid")
        gene.setValueWithRawString((max + BigInteger.ONE).toString())
        assertFalse(gene.isLocallyValid(), "value above max (${max + BigInteger.ONE}) should be invalid")
    }

    @Test
    fun testUint8() {
        val gene = buildFormatGene("integer", "uint8")
        assertTrue(gene is IntegerGene)
        (gene as IntegerGene).apply {
            assertEquals(0, getMinimum())
            assertEquals(255, getMaximum())
        }
        assertAlwaysInRange(gene, BigInteger.ZERO, BigInteger.valueOf(255))
        assertBoundaries(gene as SimpleGene,BigInteger.ZERO, BigInteger.valueOf(255))
    }

    @Test
    fun testUint16() {
        val gene = buildFormatGene("integer", "uint16")
        assertTrue(gene is IntegerGene)
        (gene as IntegerGene).apply {
            assertEquals(0, getMinimum())
            assertEquals(65535, getMaximum())
        }
        assertAlwaysInRange(gene, BigInteger.ZERO, BigInteger.valueOf(65535))
        assertBoundaries(gene as SimpleGene,BigInteger.ZERO, BigInteger.valueOf(65535))
    }

    @Test
    fun testUint32() {
        val gene = buildFormatGene("integer", "uint32")
        assertTrue(gene is LongGene)
        (gene as LongGene).apply {
            assertEquals(0L, getMinimum())
            assertEquals(4294967295L, getMaximum())
        }
        assertAlwaysInRange(gene, BigInteger.ZERO, BigInteger.valueOf(4294967295L))
        assertBoundaries(gene as SimpleGene,BigInteger.ZERO, BigInteger.valueOf(4294967295L))
    }

    @Test
    fun testUint64() {
        val gene = buildFormatGene("integer", "uint64")
        assertTrue(gene is BigIntegerGene)
        // BigIntegerGene is internally Long-bounded, so uint64 is capped at Long.MAX_VALUE
        val max = BigInteger.valueOf(Long.MAX_VALUE)
        (gene as BigIntegerGene).apply {
            assertEquals(BigInteger.ZERO, getMinimum())
            assertEquals(max, getMaximum())
        }
        assertAlwaysInRange(gene, BigInteger.ZERO, max)
    }

    @Test
    fun testInt8() {
        val gene = buildFormatGene("integer", "int8")
        assertTrue(gene is IntegerGene)
        (gene as IntegerGene).apply {
            assertEquals(-128, getMinimum())
            assertEquals(127, getMaximum())
        }
        assertAlwaysInRange(gene, BigInteger.valueOf(-128), BigInteger.valueOf(127))
        assertBoundaries(gene as SimpleGene,BigInteger.valueOf(-128), BigInteger.valueOf(127))
    }

    /**
     * Every value the gene produces must be valid according to an INDEPENDENT, authoritative
     * oracle (a real decoder or the actual RFC rule), not a mirror of the generation regex.
     * If the implementation emits a malformed value, [isValid] must reject it and the test fails.
     */
    private fun assertAllGeneratedValid(gene: Gene, isValid: (String) -> Boolean) {
        val rand = Randomness()
        if (!gene.initialized) gene.doInitialize(rand)
        for (i in 0..2000) {
            gene.randomize(rand, false)
            val v = gene.getValueAsRawString()
            assertTrue(isValid(v), "Implementation generated an invalid value: '$v'")
        }
    }

    // ---- Authoritative oracles (independent of the generation regexes) ----

    /** RFC4648 base64url: decodes without error. The JDK decoder rejects e.g. length 4k+1. */
    private fun isValidBase64Url(v: String): Boolean {
        if (v.isEmpty()) return false
        return try {
            java.util.Base64.getUrlDecoder().decode(v)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** RFC1123 host name: 1..253 chars, dot-separated labels, each 1..63 of [A-Za-z0-9-], no edge hyphen. */
    private fun isValidRfc1123Hostname(v: String): Boolean {
        if (v.isEmpty() || v.length > 253) return false
        val label = Regex("[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?")
        return v.split(".").all { it.matches(label) }
    }

    /** RFC6901 JSON pointer: empty, or '/'-separated tokens where '~' is only '~0' or '~1'. */
    private fun isValidJsonPointer(v: String): Boolean {
        if (v.isEmpty()) return true
        if (!v.startsWith("/")) return false
        for (token in v.substring(1).split("/")) {
            var i = 0
            while (i < token.length) {
                if (token[i] == '~') {
                    if (i + 1 >= token.length || (token[i + 1] != '0' && token[i + 1] != '1')) return false
                    i += 2
                } else i++
            }
        }
        return true
    }

    /** RFC9110 media-range: type '/' subtype, each a non-empty RFC7230 token (or '*'). */
    private fun isValidMediaRange(v: String): Boolean {
        val parts = v.split("/")
        if (parts.size != 2) return false
        val token = Regex("[A-Za-z0-9!#%&'*+.^_|~-]+")
        return parts.all { it == "*" || it.matches(token) }
    }

    /** RFC3986 URI reference: parses with java.net.URI without error. */
    private fun isValidUriReference(v: String): Boolean {
        return try {
            java.net.URI(v)
            true
        } catch (e: java.net.URISyntaxException) {
            false
        }
    }

    /** A valid ECMA-262-ish regex: compiles with java.util.regex.Pattern. */
    private fun isValidRegex(v: String): Boolean {
        return try {
            java.util.regex.Pattern.compile(v)
            true
        } catch (e: java.util.regex.PatternSyntaxException) {
            false
        }
    }

    /** RFC8941 structured-field boolean: exactly '?0' or '?1'. */
    private fun isValidSfBoolean(v: String): Boolean = v == "?0" || v == "?1"

    /** RFC3987 IRI: an absolute reference that java.net.URI can parse. */
    private fun isValidIri(v: String): Boolean {
        return try {
            java.net.URI(v).isAbsolute
        } catch (e: java.net.URISyntaxException) {
            false
        }
    }

    /** Structural email check: exactly one '@', non-empty parts, dotted domain, no spaces/controls. */
    private fun isValidIdnEmail(v: String): Boolean {
        val at = v.indexOf('@')
        if (at <= 0 || at != v.lastIndexOf('@')) return false
        val domain = v.substring(at + 1)
        if (domain.isEmpty() || !domain.contains('.') || domain.startsWith('.') || domain.endsWith('.')) return false
        return v.none { it == ' ' || it.code < 0x20 }
    }

    /** Relative JSON pointer: non-negative integer (no leading zeros), then "", "#", or a JSON pointer. */
    private fun isValidRelativeJsonPointer(v: String): Boolean {
        var i = 0
        while (i < v.length && v[i].isDigit()) i++
        if (i == 0) return false
        val num = v.substring(0, i)
        if (num.length > 1 && num[0] == '0') return false
        val rest = v.substring(i)
        return rest.isEmpty() || rest == "#" || isValidJsonPointer(rest)
    }

    /** RFC8941 sf-string: printable ASCII in double quotes; '\' only escapes '\' or '"'. */
    private fun isValidSfString(v: String): Boolean {
        if (v.length < 2 || v.first() != '"' || v.last() != '"') return false
        val inner = v.substring(1, v.length - 1)
        var i = 0
        while (i < inner.length) {
            val c = inner[i]
            when {
                c == '\\' -> {
                    if (i + 1 >= inner.length || (inner[i + 1] != '\\' && inner[i + 1] != '"')) return false
                    i += 2
                }
                c == '"' || c.code < 0x20 || c.code > 0x7e -> return false
                else -> i++
            }
        }
        return true
    }

    /** RFC8941 sf-token: first char ALPHA or '*', then tchar / ':' / '/'. */
    private fun isValidSfToken(v: String): Boolean {
        if (v.isEmpty()) return false
        val first = v[0]
        if (!(first in 'A'..'Z' || first in 'a'..'z' || first == '*')) return false
        val tchar = "!#\$%&'*+-.^_`|~"
        for (i in 1 until v.length) {
            val c = v[i]
            val ok = c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c in tchar || c == ':' || c == '/'
            if (!ok) return false
        }
        return true
    }

    /**
     * RFC6570 URI template: every {expr} matches a valid expression grammar, and the template
     * with all expressions replaced by a placeholder is a real URI (parsed by java.net.URI).
     */
    private fun isValidUriTemplate(v: String): Boolean {
        val expr = Regex("\\{[+#./;?&]?[a-zA-Z0-9_]+(\\*|:[0-9]+)?(,[a-zA-Z0-9_]+(\\*|:[0-9]+)?)*\\}")
        val skeleton = expr.replace(v, "x")
        if (skeleton.contains('{') || skeleton.contains('}')) return false
        return try {
            java.net.URI(skeleton)
            true
        } catch (e: java.net.URISyntaxException) {
            false
        }
    }

    /** RFC8941 sf-binary: standard base64 wrapped in ':' that decodes without error. */
    private fun isValidSfBinary(v: String): Boolean {
        if (v.length < 2 || v.first() != ':' || v.last() != ':') return false
        val inner = v.substring(1, v.length - 1)
        if (inner.isEmpty()) return false
        return try {
            java.util.Base64.getDecoder().decode(inner)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /** A valid decimal number: parses with java.math.BigDecimal. */
    private fun isValidDecimal(v: String): Boolean {
        return try {
            java.math.BigDecimal(v)
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    /** decimal128: a decimal with at most 34 significant digits. */
    private fun isValidDecimal128(v: String): Boolean {
        val bd = try {
            java.math.BigDecimal(v)
        } catch (e: NumberFormatException) {
            return false
        }
        return bd.precision() <= 34
    }

    /** RFC8941 sf-decimal: at most 12 integer digits and 3 fractional digits. */
    private fun isValidSfDecimal(v: String): Boolean {
        val bd = try {
            java.math.BigDecimal(v)
        } catch (e: NumberFormatException) {
            return false
        }
        if (bd.scale() > 3) return false
        val integerDigits = bd.precision() - bd.scale()
        return integerDigits <= 12
    }

    /**
     * RFC3339 full-time: partial-time + ('Z' or +/-HH:MM).
     * Note: RFC3339 allows offsets up to +/-23:59 (wider than java.time.ZoneOffset's +/-18:00),
     * so the offset range is checked structurally and only the time part is parsed with LocalTime.
     */
    private fun isValidTime(v: String): Boolean {
        val m = Regex("(.+?)(Z|[+-][0-9]{2}:[0-9]{2})").matchEntire(v) ?: return false
        val offset = m.groupValues[2]
        if (offset != "Z") {
            val h = offset.substring(1, 3).toInt()
            val min = offset.substring(4, 6).toInt()
            if (h > 23 || min > 59) return false
        }
        return try {
            java.time.LocalTime.parse(m.groupValues[1])
            true
        } catch (e: java.time.format.DateTimeParseException) {
            false
        }
    }

    /** Local time (no timezone): parses with java.time.LocalTime. */
    private fun isValidTimeLocal(v: String): Boolean {
        return try {
            java.time.LocalTime.parse(v)
            true
        } catch (e: java.time.format.DateTimeParseException) {
            false
        }
    }

    /** Local date-time (no timezone): parses with java.time.LocalDateTime. */
    private fun isValidDateTimeLocal(v: String): Boolean {
        return try {
            java.time.LocalDateTime.parse(v)
            true
        } catch (e: java.time.format.DateTimeParseException) {
            false
        }
    }

    /** ISO-8601 / RFC3339 duration: parses with javax.xml.datatype (handles full P..Y..M..DT..H..M..S). */
    private fun isValidDuration(v: String): Boolean {
        return try {
            javax.xml.datatype.DatatypeFactory.newInstance().newDuration(v)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Non-negative epoch seconds. */
    private fun isValidUnixtime(v: String): Boolean = v.toLongOrNull()?.let { it >= 0 } ?: false

    /** Integer exactly representable as an IEEE-754 double: |n| <= 2^53. */
    private fun isValidDoubleInt(v: String): Boolean =
        v.toLongOrNull()?.let { kotlin.math.abs(it) <= 9007199254740992L } ?: false

    /** RFC8941 sf-integer: at most 15 digits. */
    private fun isValidSfInteger(v: String): Boolean =
        v.toLongOrNull()?.let { it in -999999999999999L..999999999999999L } ?: false

    /** IPv4 dotted-quad: exactly four octets each 0..255. */
    private fun isValidIpv4(v: String): Boolean {
        val parts = v.split(".")
        if (parts.size != 4) return false
        return parts.all { it.isNotEmpty() && it.all(Char::isDigit) && it.toInt() in 0..255 }
    }

    /** IPv6: parses to an Inet6Address (numeric literal, so no DNS lookup happens). */
    private fun isValidIpv6(v: String): Boolean {
        return try {
            java.net.InetAddress.getByName(v) is java.net.Inet6Address
        } catch (e: java.net.UnknownHostException) {
            false
        }
    }

    /** RFC7231 IMF-fixdate: valid weekday token + a real calendar date/time + " GMT". */
    private fun isValidHttpDate(v: String): Boolean {
        val m = Regex("(Mon|Tue|Wed|Thu|Fri|Sat|Sun), (.+) GMT").matchEntire(v) ?: return false
        return try {
            java.time.LocalDateTime.parse(
                m.groupValues[2],
                java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", java.util.Locale.ENGLISH)
            )
            true
        } catch (e: java.time.format.DateTimeParseException) {
            false
        }
    }

    @Test
    fun testHostname() {
        val gene = buildFormatGene("string", "hostname")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidRfc1123Hostname)
    }

    @Test
    fun testIdnHostname() {
        val gene = buildFormatGene("string", "idn-hostname")
        assertTrue(gene is RegexGene)
        // we currently sample the ASCII subset, so an RFC1123 host name is a valid idn-hostname
        assertAllGeneratedValid(gene, ::isValidRfc1123Hostname)
    }

    @Test
    fun testBase64Url() {
        val gene = buildFormatGene("string", "base64url")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidBase64Url)
    }

    @Test
    fun testJsonPointer() {
        val gene = buildFormatGene("string", "json-pointer")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidJsonPointer)
    }

    @Test
    fun testMediaRange() {
        val gene = buildFormatGene("string", "media-range")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidMediaRange)
    }

    @Test
    fun testUriReference() {
        val gene = buildFormatGene("string", "uri-reference")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidUriReference)
    }

    @Test
    fun testIriReference() {
        val gene = buildFormatGene("string", "iri-reference")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidUriReference)
    }

    @Test
    fun testRegexFormat() {
        val gene = buildFormatGene("string", "regex")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidRegex)
    }

    @Test
    fun testSfBoolean() {
        val gene = buildFormatGene("string", "sf-boolean")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidSfBoolean)
    }

    @Test
    fun testIri() {
        val gene = buildFormatGene("string", "iri")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidIri)
    }

    @Test
    fun testIdnEmail() {
        val gene = buildFormatGene("string", "idn-email")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidIdnEmail)
    }

    @Test
    fun testRelativeJsonPointer() {
        val gene = buildFormatGene("string", "relative-json-pointer")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidRelativeJsonPointer)
    }

    @Test
    fun testSfString() {
        val gene = buildFormatGene("string", "sf-string")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidSfString)
    }

    @Test
    fun testSfToken() {
        val gene = buildFormatGene("string", "sf-token")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidSfToken)
    }

    @Test
    fun testSfBinary() {
        val gene = buildFormatGene("string", "sf-binary")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidSfBinary)
    }

    @Test
    fun testUriTemplate() {
        val gene = buildFormatGene("string", "uri-template")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidUriTemplate)
    }

    @Test
    fun testDecimal() {
        val gene = buildFormatGene("number", "decimal")
        assertTrue(gene is BigDecimalGene)
        assertAllGeneratedValid(gene, ::isValidDecimal)
    }

    @Test
    fun testDecimal128() {
        val gene = buildFormatGene("number", "decimal128")
        assertTrue(gene is BigDecimalGene)
        assertAllGeneratedValid(gene, ::isValidDecimal128)
    }

    @Test
    fun testSfDecimal() {
        val gene = buildFormatGene("number", "sf-decimal")
        assertTrue(gene is BigDecimalGene)
        assertAllGeneratedValid(gene, ::isValidSfDecimal)
    }

    @Test
    fun testTime() {
        val gene = buildFormatGene("string", "time")
        assertTrue(gene is TimeGene)
        assertAllGeneratedValid(gene, ::isValidTime)
    }

    @Test
    fun testTimeLocal() {
        val gene = buildFormatGene("string", "time-local")
        assertTrue(gene is TimeGene)
        assertAllGeneratedValid(gene, ::isValidTimeLocal)
    }

    @Test
    fun testDateTimeLocal() {
        val gene = buildFormatGene("string", "date-time-local")
        assertTrue(gene is DateTimeGene)
        assertAllGeneratedValid(gene, ::isValidDateTimeLocal)
    }

    @Test
    fun testDuration() {
        val gene = buildFormatGene("string", "duration")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidDuration)
    }

    @Test
    fun testHttpDate() {
        val gene = buildFormatGene("string", "http-date")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidHttpDate)
    }

    @Test
    fun testIpv4() {
        val gene = buildFormatGene("string", "ipv4")
        assertTrue(gene is InetGene)
        assertAllGeneratedValid(gene, ::isValidIpv4)
    }

    @Test
    fun testIpv6() {
        val gene = buildFormatGene("string", "ipv6")
        assertTrue(gene is RegexGene)
        assertAllGeneratedValid(gene, ::isValidIpv6)
    }

    @Test
    fun testUnixtime() {
        val gene = buildFormatGene("integer", "unixtime")
        assertTrue(gene is LongGene)
        assertAllGeneratedValid(gene, ::isValidUnixtime)
    }

    @Test
    fun testDoubleInt() {
        val gene = buildFormatGene("number", "double-int")
        assertTrue(gene is LongGene)
        assertAllGeneratedValid(gene, ::isValidDoubleInt)
    }

    @Test
    fun testSfInteger() {
        val gene = buildFormatGene("integer", "sf-integer")
        assertTrue(gene is LongGene)
        assertAllGeneratedValid(gene, ::isValidSfInteger)
    }

    // commonmark/html: any string is valid markup text, so we only assert the gene type.
    @Test
    fun testCommonmark() {
        val gene = buildFormatGene("string", "commonmark")
        assertTrue(gene is StringGene)
    }

    @Test
    fun testHtml() {
        val gene = buildFormatGene("string", "html")
        assertTrue(gene is StringGene)
    }

    /**
     * Sanity check that the base64url oracle actually has teeth: values of length 4k+1
     * (like the single-char values the old implementation produced) MUST be rejected.
     */
    @Test
    fun testBase64UrlOracleRejectsInvalid() {
        assertFalse(isValidBase64Url("-"))   // length 1
        assertFalse(isValidBase64Url("y"))   // length 1
        assertFalse(isValidBase64Url("abcde")) // length 5 = 4k+1
        assertTrue(isValidBase64Url("ZH"))   // length 2 -> 1 byte
        assertTrue(isValidBase64Url("qGm"))  // length 3 -> 2 bytes
    }

    @Test
    fun testInt16() {
        val gene = buildFormatGene("integer", "int16")
        assertTrue(gene is IntegerGene)
        (gene as IntegerGene).apply {
            assertEquals(-32768, getMinimum())
            assertEquals(32767, getMaximum())
        }
        assertAlwaysInRange(gene, BigInteger.valueOf(-32768), BigInteger.valueOf(32767))
        assertBoundaries(gene as SimpleGene,BigInteger.valueOf(-32768), BigInteger.valueOf(32767))
    }

    /**
     * Diagnostic (not an assertion test): print a few sampled values per format so one can
     * eyeball whether the generated data looks right.
     */
    @Test
    @Disabled("for manual inspection, not a test")
    fun printSamples() {
        val rand = Randomness()
        val formats = listOf(
            "integer" to "uint8", "integer" to "uint16", "integer" to "uint32", "integer" to "uint64",
            "string" to "hostname", "string" to "idn-hostname", "string" to "base64url",
            "string" to "json-pointer", "string" to "media-range",
            "string" to "uri-reference", "string" to "iri-reference",
            "string" to "regex", "string" to "sf-boolean",
            "string" to "iri", "string" to "idn-email", "string" to "relative-json-pointer",
            "string" to "sf-string", "string" to "sf-token", "string" to "sf-binary",
            "string" to "uri-template",
            "number" to "decimal", "number" to "decimal128", "number" to "sf-decimal",
            "string" to "time", "string" to "time-local", "string" to "date-time-local",
            "string" to "duration", "string" to "http-date",
            "string" to "ipv4", "string" to "ipv6",
            "integer" to "unixtime", "number" to "double-int", "integer" to "sf-integer",
            "string" to "commonmark", "string" to "html"
        )
        for ((type, format) in formats) {
            val gene = buildFormatGene(type, format)
            if (!gene.initialized) gene.doInitialize(rand)
            val samples = (1..8).map {
                gene.randomize(rand, false)
                gene.getValueAsRawString()
            }
            println("[$format] -> $samples")
        }
    }

}
