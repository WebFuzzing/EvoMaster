package org.evomaster.e2etests.spring.rest.bb.multipart

import com.foo.rest.examples.bb.multipart.BBMultipartController
import org.evomaster.core.EMConfig
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.rest.bb.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/**
 * Black-box e2e for multipart/form-data support.
 *
 * The @EnumSource over OutputFormat is what makes this cover the whole writer side: for every output
 * format, the suite is generated, COMPILED and RUN. That is the half of the feature the unit tests
 * cannot reach — in particular whether the envelope, which contains CRLFs and double quotes, survives
 * being embedded as a string literal in each dialect.
 */
class BBMultipartEMTest : SpringTestBase() {

    companion object {

        @BeforeAll
        @JvmStatic
        fun init() {
            val config = EMConfig()
            initClass(BBMultipartController(), config)
        }
    }

    @ParameterizedTest
    @EnumSource
    fun testBlackBoxOutput(outputFormat: OutputFormat) {

        executeAndEvaluateBBTest(
            outputFormat,
            "BBMultipartEM",
            1000,
            3,
            /*
                The harness asserts this is EXACTLY the set of targets the generated tests cover, so it
                doubles as a statement of what the feature can and cannot reach. Deliberately absent:
                  - MP_CONTENT_VALID, because the content of a part is arbitrary text and never a real
                    image: that is the documented limitation of this version;
                  - MP_EXTENSION_REJECTED and MP_NO_EXTENSION, because the filename is derived from the
                    declared encoding and is therefore always "file.png" here.
             */
            listOf(
                "MP_TEXT_ONLY",
                "MP_STORE_ONLY",
                "MP_EXTENSION_ACCEPTED",
                "MP_CONTENT_INVALID",
                "MP_ONLY"
            )
        ) { args: MutableList<String> ->

            setOption(args, "enableMultipartFormDataSupport", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            /*
                1. Text parts only. Isolates the envelope itself: a 200 here means the multipart body is
                   well formed, independently of anything to do with filenames.
             */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbmp/text", null)

            /*
                2. Store-only upload. Spring injects a MultipartFile only if the part carries a filename,
                   so a 200 here proves the Content-Disposition was emitted correctly.
                   This is the case the whole feature bets on.
             */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbmp/store", null)

            /*
                4. Parses the content. The content of a part is arbitrary text, never a real image, so
                   this must NOT get past validation. Asserting the 400 documents that limitation with
                   evidence instead of prose.
             */
            assertHasAtLeastOne(solution, HttpVerb.POST, 400, "/api/bbmp/validate", null)

            /*
                5. Accepts only multipart: with urlencoded, Spring answers 415 before reaching the
                   method. A 200 is therefore the direct proof that a real multipart request is sent,
                   and that the old substitution to urlencoded no longer applies.
             */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbmp/multipart-only", null)

            /*
                3. Extension whitelist. The schema declares encoding.contentType = image/png for the
                   file part, so the derived filename is "file.png" and the whitelist accepts it.
             */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/bbmp/whitelist", null)
        }
    }

    /**
     * With the flag off, the behaviour must be the one from before the feature: the content type gene
     * still offers multipart, but the body is serialized as TEXT with no boundary, so every multipart
     * endpoint rejects it. The point is that nothing crashes and no coverage is gained.
     */
    @Test
    fun testBlackBoxWithoutMultipartSupport() {

        runBlackBoxEM(OutputFormat.KOTLIN_JUNIT_5, "BBMultipartEM_NoSupport", 1000, 3, false) { args ->

            setOption(args, "enableMultipartFormDataSupport", "false")

            val solution = initAndRun(args)
            assertTrue(solution.individuals.size >= 1)
        }
    }
}
