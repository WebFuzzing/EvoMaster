package org.evomaster.e2etests.spring.multidb.separatedschemas

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.driver.multidb.SpringController
import org.evomaster.e2etests.spring.multidb.MultiDbParameterizedE2ETemplate
import org.junit.jupiter.api.Assertions.assertTrue
import org.evomaster.driver.multidb.separatedschemas.SeparatedSchemasController

/**
 * Created by arcuri82 on 03-Mar-20.
 */
class SeparatedSchemasEMTest : MultiDbParameterizedE2ETemplate() {

    override fun instantiateNewController(): SpringController {
        return SeparatedSchemasController()
    }

    override fun runEM(databaseType: DatabaseType) {

        runTestHandlingFlakyAndCompilation(
                "SeparatedSchemasEM_$databaseType",
                100
        ) { args: MutableList<String> ->

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/separatedschemas/x/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 404, "/api/separatedschemas/y/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/separatedschemas/x/{id}", "OK")
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/separatedschemas/y/{id}", "OK")
        }
    }
}