package org.evomaster.e2etests.spring.rest.polyglotpersistence

import com.foo.spring.rest.polyglotpersistence.PolyglotPersistenceSutController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PolyglotPersistenceEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            val config = EMConfig()
            config.instrumentMR_MONGO = true
            config.instrumentMR_REDIS = true
            initClass(PolyglotPersistenceSutController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "MultiDbMongoRedisPostgresEM",
            1000
        ) { args ->
            // enable heuristics for SQL, MongoDB and Redis
            setOption(args, "instrumentMR_MONGO", "true")
            setOption(args, "instrumentMR_REDIS", "true")
            setOption(args, "instrumentMR_SQL", "true")

            setOption(args, "extractSqlExecutionInfo", "true")
            setOption(args, "extractMongoExecutionInfo", "true")
            setOption(args, "extractRedisExecutionInfo", "true")

            setOption(args, "heuristicsForSQL", "true")
            setOption(args, "heuristicsForMongo", "true")
            setOption(args, "heuristicsForRedis", "true")
            // enable data insertion for SQL, MongoDB, and Redis
            setOption(args, "generateSqlDataWithSearch", "true")
            setOption(args, "generateMongoData", "true")
            setOption(args, "generateRedisData", "true")

            val solution = initAndRun(args)

            assertTrue(solution.individuals.size >= 1)

            // Combined GET endpoint
            assertHasAtLeastOne(solution, HttpVerb.GET, 400, "/api/get/{idsql}/{idmongo}/{idredis}", null)
            assertHasAtLeastOne(solution, HttpVerb.GET, 200, "/api/get/{idsql}/{idmongo}/{idredis}", null)

            // POST endpoints for each database
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/postgres/{id}", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/mongo/{age}", null)
            assertHasAtLeastOne(solution, HttpVerb.POST, 200, "/api/redis/{key}", null)
        }
    }
}
