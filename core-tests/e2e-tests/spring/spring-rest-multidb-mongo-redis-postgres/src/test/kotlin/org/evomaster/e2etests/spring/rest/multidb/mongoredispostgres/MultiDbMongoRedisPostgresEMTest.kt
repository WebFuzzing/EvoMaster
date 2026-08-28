package org.evomaster.e2etests.spring.rest.multidb.mongoredispostgres

import com.foo.spring.rest.multidb.mongoredispostgres.MultiDbMongoRedisPostgresSutController
import org.evomaster.core.EMConfig
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.e2etests.utils.RestTestBase
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class MultiDbMongoRedisPostgresEMTest : RestTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun initClass() {
            val config = EMConfig()
            config.instrumentMR_MONGO = true
            config.instrumentMR_REDIS = true
            RestTestBase.initClass(MultiDbMongoRedisPostgresSutController(), config)
        }
    }

    @Test
    fun testRunEM() {
        runTestHandlingFlakyAndCompilation(
            "MultiDbMongoRedisPostgresEM",
            1000
        ) { args ->
            // disable impact analysis for this test, as it is not working properly with the current implementation of the SUT
            setOption(args, "doCollectImpact", "false")
            setOption(args, "adaptiveGeneSelectionMethod", "NONE")
            setOption(args, "archiveGeneMutation", "NONE")
            setOption(args, "probOfArchiveMutation", "0.0")
            // enable heuristics for SQL, MongoDB and Redis
            setOption(args, "heuristicsForSQL", "true")
            setOption(args, "instrumentMR_SQL", "true")
            setOption(args, "heuristicsForMongo", "true")
            setOption(args, "instrumentMR_MONGO", "true")
            setOption(args, "heuristicsForRedis", "true")
            setOption(args, "instrumentMR_REDIS", "true")
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
