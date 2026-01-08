package org.evomaster.e2etests.spring.rest.postgres.json

import com.foo.spring.rest.postgres.json.JsonColumnController
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.e2etests.spring.rest.postgres.SpringRestPostgresTestBase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class JsonColumnEMTest : SpringRestPostgresTestBase(){

    companion object {
            @BeforeAll @JvmStatic
            fun initClass() {
                initKlass(JsonColumnController())
            }
        }

        @Test
        fun testRunEM() {

            runTestHandlingFlakyAndCompilation(
                    "JsonColumnEM",
                    "org.bar.JsonColumnEM",
                    10
            ) { args ->
                val solution = initAndRun(args)

                assertTrue(solution.individuals.size >= 1)

                assertHasAtLeastOne(solution, HttpVerb.GET, 400,"/api/json/fromJson",null)
                // TODO replace this assert with assertHasAtLeastOne() when postgresql json column type is implemented
                assertFalse(solution.individuals.stream().anyMatch { ind: EvaluatedIndividual<*>? ->
                    hasAtLeastOne(
                        ind as EvaluatedIndividual<RestIndividual>?,
                        HttpVerb.GET,
                        200,
                        "/api/json/fromJson",
                        null
                    )
                })
            }
    }
}