package com.foo.rest.examples.bb.advancedformats

import org.evomaster.e2etests.utils.CoveredTargets
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.xml.datatype.DatatypeFactory

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@RequestMapping(path = ["/api/advancedformats"])
@RestController
open class BBAdvancedFormatsApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(BBAdvancedFormatsApplication::class.java, *args)
        }
    }


    @GetMapping("/uuid")
    open fun getUuid(
        @RequestParam(required = true) x: String?
    ) : ResponseEntity<String> {

        if (x == null) {
            return ResponseEntity.status(400).build()
        }

        UUID.fromString(x)

        CoveredTargets.cover("uuid")

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/email")
    open fun getEmail(
        @RequestParam(required = true) x: String?
    ) : ResponseEntity<String> {

        if (x == null || !x.contains('@') || !x.contains('.')) {
            return ResponseEntity.status(400).build()
        }

        CoveredTargets.cover("email")

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/uri")
    open fun getUri(
        @RequestParam(required = true) x: String?
    ) : ResponseEntity<String> {

        if (x == null) {
            return ResponseEntity.status(400).build()
        }

        URI(x)

        CoveredTargets.cover("uri")

        return ResponseEntity.status(200).body("OK")
    }

    private fun coverIfInRange(
        x: String?,
        target: String,
        min: BigInteger,
        max: BigInteger
    ): ResponseEntity<String> {

        if (x == null) {
            return ResponseEntity.status(400).build()
        }

        val value = try {
            BigInteger(x)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        }

        if (value < min || value > max) {
            return ResponseEntity.status(400).build()
        }

        CoveredTargets.cover(target)

        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/int8")
    open fun getInt8(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "int8", BigInteger.valueOf(-128), BigInteger.valueOf(127))

    @GetMapping("/int16")
    open fun getInt16(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "int16", BigInteger.valueOf(-32768), BigInteger.valueOf(32767))

    @GetMapping("/uint8")
    open fun getUint8(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "uint8", BigInteger.ZERO, BigInteger.valueOf(255))

    @GetMapping("/uint16")
    open fun getUint16(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "uint16", BigInteger.ZERO, BigInteger.valueOf(65535))

    @GetMapping("/uint32")
    open fun getUint32(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "uint32", BigInteger.ZERO, BigInteger.valueOf(4294967295L))

    @GetMapping("/uint64")
    open fun getUint64(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "uint64", BigInteger.ZERO, BigInteger("18446744073709551615"))

    private fun coverIfMatches(
        x: String?,
        target: String,
        regex: String
    ): ResponseEntity<String> {

        if (x == null || !Regex(regex).matches(x)) {
            return ResponseEntity.status(400).build()
        }

        CoveredTargets.cover(target)

        return ResponseEntity.status(200).body("OK")
    }

    private fun coverIfHostname(x: String?, target: String): ResponseEntity<String> {
        if (x == null || x.isEmpty() || x.startsWith(".") || x.endsWith(".")
            || !Regex("[A-Za-z0-9.-]+").matches(x)) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover(target)
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/hostname")
    open fun getHostname(@RequestParam(required = true) x: String?) =
        coverIfHostname(x, "hostname")

    @GetMapping("/idn-hostname")
    open fun getIdnHostname(@RequestParam(required = true) x: String?) =
        coverIfHostname(x, "idn-hostname")

    @GetMapping("/base64url")
    open fun getBase64Url(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null || x.isEmpty()) {
            return ResponseEntity.status(400).build()
        }
        try {
            Base64.getUrlDecoder().decode(x)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("base64url")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/json-pointer")
    open fun getJsonPointer(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null || (x.isNotEmpty() && !x.startsWith("/"))) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("json-pointer")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/media-range")
    open fun getMediaRange(@RequestParam(required = true) x: String?) =
        coverIfMatches(x, "media-range", "[a-z]+/[a-z0-9.+-]+")

    private fun coverIfUriReference(x: String?, target: String): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        try {
            URI(x)
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover(target)
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/uri-reference")
    open fun getUriReference(@RequestParam(required = true) x: String?) =
        coverIfUriReference(x, "uri-reference")

    @GetMapping("/iri-reference")
    open fun getIriReference(@RequestParam(required = true) x: String?) =
        coverIfUriReference(x, "iri-reference")

    @GetMapping("/regex")
    open fun getRegex(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        try {
            Pattern.compile(x)
        } catch (e: PatternSyntaxException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("regex")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/sf-boolean")
    open fun getSfBoolean(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x != "?0" && x != "?1") {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("sf-boolean")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/iri")
    open fun getIri(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        val absolute = try {
            URI(x).isAbsolute
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }
        if (!absolute) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("iri")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/idn-email")
    open fun getIdnEmail(@RequestParam(required = true) x: String?) =
        coverIfMatches(x, "idn-email", "[^@ ]+@[^@ ]+\\.[^@ ]+")

    @GetMapping("/relative-json-pointer")
    open fun getRelativeJsonPointer(@RequestParam(required = true) x: String?) =
        coverIfMatches(x, "relative-json-pointer", "(0|[1-9][0-9]*)(#|(/[A-Za-z0-9]+)*)")

    @GetMapping("/sf-string")
    open fun getSfString(@RequestParam(required = true) x: String?) =
        coverIfMatches(x, "sf-string", "\"[A-Za-z0-9 ]*\"")

    @GetMapping("/sf-token")
    open fun getSfToken(@RequestParam(required = true) x: String?) =
        coverIfMatches(x, "sf-token", "[A-Za-z*][A-Za-z0-9!#%&'*+.^_|~:/-]*")

    @GetMapping("/sf-binary")
    open fun getSfBinary(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null || x.length < 2 || !x.startsWith(":") || !x.endsWith(":")) {
            return ResponseEntity.status(400).build()
        }
        try {
            Base64.getDecoder().decode(x.substring(1, x.length - 1))
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("sf-binary")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/decimal")
    open fun getDecimal(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        try {
            BigDecimal(x)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("decimal")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/decimal128")
    open fun getDecimal128(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        val bd = try {
            if (x == null) return ResponseEntity.status(400).build()
            BigDecimal(x)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        }
        if (bd.precision() > 34) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("decimal128")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/sf-decimal")
    open fun getSfDecimal(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        val bd = try {
            if (x == null) return ResponseEntity.status(400).build()
            BigDecimal(x)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        }
        if (bd.scale() > 3 || bd.precision() - bd.scale() > 12) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("sf-decimal")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/uri-template")
    open fun getUriTemplate(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        val expr = Regex("\\{[+#./;?&]?[A-Za-z0-9_]+(\\*|:[0-9]+)?(,[A-Za-z0-9_]+(\\*|:[0-9]+)?)*\\}")
        val skeleton = expr.replace(x, "x")
        if (skeleton.contains('{') || skeleton.contains('}')) {
            return ResponseEntity.status(400).build()
        }
        try {
            URI(skeleton)
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("uri-template")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/time")
    open fun getTime(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        //RFC3339 full-time: partial-time + ('Z' or +/-HH:MM up to 23:59)
        val m = Regex("(.+?)(Z|[+-][0-9]{2}:[0-9]{2})").matchEntire(x)
            ?: return ResponseEntity.status(400).build()
        val offset = m.groupValues[2]
        if (offset != "Z") {
            if (offset.substring(1, 3).toInt() > 23 || offset.substring(4, 6).toInt() > 59) {
                return ResponseEntity.status(400).build()
            }
        }
        try {
            LocalTime.parse(m.groupValues[1])
        } catch (e: DateTimeParseException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("time")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/time-local")
    open fun getTimeLocal(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        try {
            LocalTime.parse(x)
        } catch (e: DateTimeParseException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("time-local")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/date-time-local")
    open fun getDateTimeLocal(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        try {
            LocalDateTime.parse(x)
        } catch (e: DateTimeParseException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("date-time-local")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/duration")
    open fun getDuration(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        try {
            DatatypeFactory.newInstance().newDuration(x)
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("duration")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/ipv4")
    open fun getIpv4(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        val parts = x.split(".")
        val ok = parts.size == 4 && parts.all { p ->
            p.isNotEmpty() && p.all(Char::isDigit) && (p.toIntOrNull()?.let { it in 0..255 } ?: false)
        }
        if (!ok) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("ipv4")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/ipv6")
    open fun getIpv6(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        val ok = try {
            InetAddress.getByName(x) is Inet6Address
        } catch (e: UnknownHostException) {
            false
        }
        if (!ok) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("ipv6")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/http-date")
    open fun getHttpDate(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        val m = Regex("(Mon|Tue|Wed|Thu|Fri|Sat|Sun), (.+) GMT").matchEntire(x)
            ?: return ResponseEntity.status(400).build()
        try {
            LocalDateTime.parse(m.groupValues[2],
                DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.ENGLISH))
        } catch (e: DateTimeParseException) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("http-date")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/unixtime")
    open fun getUnixtime(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "unixtime", BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE))

    @GetMapping("/double-int")
    open fun getDoubleInt(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "double-int", BigInteger.valueOf(-9007199254740992L), BigInteger.valueOf(9007199254740992L))

    @GetMapping("/sf-integer")
    open fun getSfInteger(@RequestParam(required = true) x: String?) =
        coverIfInRange(x, "sf-integer", BigInteger.valueOf(-999999999999999L), BigInteger.valueOf(999999999999999L))

    @GetMapping("/commonmark")
    open fun getCommonmark(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("commonmark")
        return ResponseEntity.status(200).body("OK")
    }

    @GetMapping("/html")
    open fun getHtml(@RequestParam(required = true) x: String?): ResponseEntity<String> {
        if (x == null) {
            return ResponseEntity.status(400).build()
        }
        CoveredTargets.cover("html")
        return ResponseEntity.status(200).body("OK")
    }

}