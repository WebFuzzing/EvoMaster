package org.evomaster.e2etests.spring.openapi.v3.jackson.typereflistdto

import com.foo.rest.examples.spring.openapi.v3.jackson.mapdouble.MapDoubleJacksonController
import com.foo.rest.examples.spring.openapi.v3.jackson.mapdto.MapDtoJacksonController
import com.foo.rest.examples.spring.openapi.v3.jackson.maplistint.MapListIntJacksonController
import com.foo.rest.examples.spring.openapi.v3.jackson.typereflistdto.TypeRefListDtoJacksonController
import com.foo.rest.examples.spring.openapi.v3.jackson.typereflistint.TypeRefListIntJacksonController
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TypeRefListDtoJacksonEMTest : SpringTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            initClass(TypeRefListDtoJacksonController())
        }
    }

    @Test
    fun basicEMTest() {
        runTestHandlingFlakyAndCompilation(
            "TypeRefListDtoJacksonEM",
            500
        ) { args: List<String> ->

            setOption(args, "taintForceSelectionOfGenesWithSpecialization", "true")
            setOption(args, "discoveredInfoRewardedInFitness", "true")
            val solution = initAndRun(args)

            Assertions.assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/jackson/typereflistdto", "Working")
        }
    }
}
