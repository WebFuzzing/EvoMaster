package org.evomaster.e2etests.spring.rest.bb.advancedformats

import com.foo.rest.examples.bb.advancedformats.BBAdvancedFormatsController
import com.foo.rest.examples.bb.exampleobject.BBExampleObjectController
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.evomaster.e2etests.utils.EnterpriseTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class BBAdvancedFormatsEMTest : SpringTestBase() {

    companion object {
        init {
            shouldApplyInstrumentation = false
        }

        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBAdvancedFormatsController())
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        val targets = listOf(
            "uuid", "uri", "email",
            "int8", "int16", "uint8", "uint16", "uint32", "uint64",
            "hostname", "idn-hostname", "base64url", "json-pointer", "media-range",
            "uri-reference", "iri-reference", "regex", "sf-boolean",
            "iri", "idn-email", "relative-json-pointer", "sf-string", "sf-token", "sf-binary",
            "uri-template",
            "decimal", "decimal128", "sf-decimal",
            "time", "time-local", "date-time-local", "duration", "http-date",
            "ipv4", "ipv6",
            "unixtime", "double-int", "sf-integer", "commonmark", "html"
        )

        executeAndEvaluateBBTest(
            outputFormat,
            "advancedformats",
            1000,
            3,
            targets
        ){ args: MutableList<String> ->

            setOption(args, "schema", "$baseUrlOfSut/openapi-bbadvancedformats.json")
            setOption(args, "enableAdvancedFormats", "true")
            setOption(args, "inferFormatFromNames", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            targets.forEach {
                assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/advancedformats/$it", "OK")
            }
        }
    }
}
