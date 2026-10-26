package org.evomaster.e2etests.spring.openapi.v3.multipart

import com.foo.rest.examples.spring.openapi.v3.multipart.MultipartController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * White-box e2e for multipart/form-data support.
 *
 * Being white-box, the search is guided by instrumentation, so what is asserted here is that it
 * actually reaches branches that are only reachable through a well formed multipart request:
 *
 *  - a 200 on /form requires several text parts to arrive correctly parsed, which only happens if the
 *    envelope and its per-part Content-Disposition headers are right;
 *  - a 200 on /upload requires the part to carry a filename WITH the extension derived from the
 *    `encoding.contentType` declared in the schema (application/pdf -> .pdf). The surrounding 4xx
 *    branches are the ones a wrong filename would land on.
 */
class MultipartEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(MultipartController())
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "MultipartEM",
            "org.foo.MultipartEM",
            200
        ) { args: MutableList<String> ->

            setOption(args, "enableMultipartFormDataSupport", "true")
            /*
                These SUTs have no authentication and the POSTs return 2xx, so the security oracle would
                report fault 208 (anonymous modifications). Irrelevant to what is asserted here.
             */
            setOption(args, "security", "false")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            //the envelope itself: several text parts, correctly parsed by Spring
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/multipart/form", null)

            /*
                the upload chain: a filename must be there, with an extension, and that extension must
                match the one declared via encoding. A 200 proves all three.
             */
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/multipart/upload", null)
        }
    }
}
