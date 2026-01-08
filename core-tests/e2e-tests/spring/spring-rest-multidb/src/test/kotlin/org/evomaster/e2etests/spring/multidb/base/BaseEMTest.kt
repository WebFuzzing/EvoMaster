package org.evomaster.e2etests.spring.multidb.base

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.driver.multidb.SpringController
import org.evomaster.e2etests.spring.multidb.MultiDbParameterizedE2ETemplate
import org.junit.jupiter.api.Assertions.assertTrue
import org.evomaster.driver.multidb.base.BaseController

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class BaseEMTest : MultiDbParameterizedE2ETemplate() {

    override fun instantiateNewController(): SpringController {
        return BaseController()
    }

    override fun runEM(databaseType: DatabaseType) {
        runTestHandlingFlakyAndCompilation(
                "BaseEM_$databaseType",
                100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/base/{id}", "OK")
        }
    }
}