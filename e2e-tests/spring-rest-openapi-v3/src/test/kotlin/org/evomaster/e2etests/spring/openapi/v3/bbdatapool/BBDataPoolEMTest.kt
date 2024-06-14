package org.evomaster.e2etests.spring.openapi.v3.bbdatapool

import com.foo.rest.examples.spring.openapi.v3.bbauth.BBAuthController
import com.foo.rest.examples.spring.openapi.v3.bbdatapool.BBDataPoolController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BBDataPoolEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(BBDataPoolController())
        }
    }


    @Test
    fun testRunEMOk() {
        runTestHandlingFlakyAndCompilation(
                "BBDataPoolEM",
                200
        ) { args: MutableList<String> ->

            args.add("--blackBox")
            args.add("true")
            args.add("--bbTargetUrl")
            args.add(baseUrlOfSut)
            args.add("--bbSwaggerUrl")
            args.add("$baseUrlOfSut/v3/api-docs")
            args.add("--bbExperiments")
            args.add("false")
            args.add("--useResponseDataPool")
            args.add("true")

            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbdatapool/users", null)

            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/bbdatapool/users/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PUT, 404, "/api/bbdatapool/users/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.PATCH, 404, "/api/bbdatapool/users/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.DELETE, 404, "/api/bbdatapool/users/{id}", null)

            //with data pool, should be possible to handle the GET
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/bbdatapool/users/{id}", null)

            //but MUST NOT do it for write operations
            assertNone(solution, HttpVerb.PUT, 200, "/api/bbdatapool/users/{id}", null)
            assertNone(solution, HttpVerb.PATCH, 200, "/api/bbdatapool/users/{id}", null)
            assertNone(solution, HttpVerb.DELETE, 200, "/api/bbdatapool/users/{id}", null)
        }
    }


}