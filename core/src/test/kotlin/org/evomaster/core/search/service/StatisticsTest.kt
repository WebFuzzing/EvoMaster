package org.evomaster.core.search.service

import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.BootTimeInfoDto
import org.evomaster.client.java.controller.api.dto.TargetInfoDto
import org.evomaster.client.java.controller.api.dto.UnitsInfoDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class StatisticsTest {

    companion object {

        /**
         * How many lines an instrumented AsyncAPI driver says the SUT has.
         */
        private const val SUT_LINES = 120

        /**
         * A line the same driver says was covered while the SUT booted.
         */
        private const val BOOT_TIME_LINE = "${ObjectiveNaming.LINE}_at_com.foo.NcsApplication_00010"
    }

    /**
     * The statistics an AsyncAPI run would write, with a driver that is instrumented and says so.
     */
    private fun asyncApiStatistics(vararg options: String): Map<String, String> {

        RestActionBuilderV3.cleanCache()

        val info = AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(AsyncApiTestInjector.NCS)).apply {
            unitsInfoDto = UnitsInfoDto().apply {
                numberOfLines = SUT_LINES
                unitNames.add("com.foo.NcsApplication")
            }
            bootTimeInfoDto = BootTimeInfoDto().apply {
                targets = listOf(TargetInfoDto().apply {
                    id = 1
                    descriptiveId = BOOT_TIME_LINE
                    value = 1.0
                    actionIndex = -1
                })
            }
        }
        val injector = AsyncApiTestInjector.create(FakeAsyncApiDriver(info) { null }, *options)

        val archive = injector.getInstance(Key.get(object : TypeLiteral<Archive<AsyncApiIndividual>>() {}))
        val statistics = injector.getInstance(Statistics::class.java)

        return statistics.getData(archive.extractSolution()).associate { it.header to it.element }
    }

    @Test
    fun testAWhiteBoxAsyncApiRunReportsTheCoverageOfTheService() {

        val data = asyncApiStatistics("--blackBox=false")

        assertEquals("$SUT_LINES", data[Statistics.TOTAL_LINES])
        assertEquals("1", data[Statistics.COVERED_LINES], "the line covered at boot time is not counted")
    }

    @Test
    fun testABlackBoxAsyncApiRunReportsNoCoverageOfTheService() {

        /*
            The driver takes part only to reach the broker, and the search never asks it for
            coverage. Reporting the lines it covered at boot time out of the total it knows of
            would present a black-box run as having covered code.
         */
        val data = asyncApiStatistics("--blackBox=true")

        assertEquals("0", data[Statistics.TOTAL_LINES])
        assertEquals("0", data[Statistics.COVERED_LINES])
    }

    @Test
    fun testMongoHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(10)
        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(20)
        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(30)

        repeat (3) {
            statistics.reportMongoHeuristicEvaluationSuccess()
        }

        assertEquals(3, statistics.getMongoHeuristicsEvaluationCount())
        assertEquals((10 + 20 + 30).toDouble() / 3, statistics.averageNumberOfEvaluatedDocumentsForMongoHeuristics())
    }

    @Test
    fun testSqlHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(5)
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(7)
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(11)
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(13)

        repeat (4) {
            statistics.reportSqlHeuristicEvaluationSuccess()
        }

        assertEquals(4, statistics.getSqlHeuristicsEvaluationCount())
        assertEquals((5 + 7 + 11 + 13).toDouble() / 4, statistics.averageNumberOfEvaluatedRowsForSqlHeuristics())
    }

    @Test
    fun testDynamoDbHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedItemsForDynamoDbHeuristic(10)
        statistics.reportNumberOfEvaluatedItemsForDynamoDbHeuristic(20)
        statistics.reportNumberOfEvaluatedItemsForDynamoDbHeuristic(30)

        repeat(2) {
            statistics.reportDynamoDbHeuristicEvaluationSuccess()
        }
        statistics.reportDynamoDbHeuristicEvaluationFailure()

        assertEquals(3, statistics.getDynamoDbHeuristicsEvaluationCount())
        assertEquals(20.0, statistics.averageNumberOfEvaluatedItemsForDynamoDbHeuristics())
    }

    @Test
    fun testNeo4jHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedNodesForNeo4jHeuristic(10)
        statistics.reportNumberOfEvaluatedNodesForNeo4jHeuristic(20)
        statistics.reportNumberOfEvaluatedNodesForNeo4jHeuristic(30)

        repeat(2) {
            statistics.reportNeo4jHeuristicEvaluationSuccess()
        }
        statistics.reportNeo4jHeuristicEvaluationFailure()

        assertEquals(3, statistics.getNeo4jHeuristicsEvaluationCount())
        assertEquals(20.0, statistics.averageNumberOfEvaluatedNodesForNeo4jHeuristics())
    }

    @Test
    fun testCassandraHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedRowsForCassandraHeuristic(2)
        statistics.reportNumberOfEvaluatedRowsForCassandraHeuristic(4)

        statistics.reportCassandraHeuristicEvaluationSuccess()
        statistics.reportCassandraHeuristicEvaluationSuccess()
        statistics.reportCassandraHeuristicEvaluationFailure()

        assertEquals(3, statistics.getCassandraHeuristicsEvaluationCount())
        assertEquals((2 + 4).toDouble() / 2, statistics.averageNumberOfEvaluatedRowsForCassandraHeuristics())
    }

    @Test
    fun testSqlZ3CacheAccountingInvariant() {
        val statistics = Statistics()

        // Query #1: first sighting -> cache miss, solved as SAT
        statistics.reportSqlZ3QuerySeen(1)
        statistics.reportSqlZ3Sat(10)

        // Query #2: first sighting -> cache miss, solved as UNSAT
        statistics.reportSqlZ3QuerySeen(2)
        statistics.reportSqlZ3Unsat(5)

        // Query #1 seen again -> served from the memoization cache (a hit, not a miss)
        statistics.reportSqlZ3QuerySeen(1)
        statistics.reportSqlZ3CacheHit()

        assertEquals(3, statistics.getSqlZ3QueriesSeenCount())
        assertEquals(1, statistics.getSqlZ3CacheHitCount())
        assertEquals(2, statistics.getSqlZ3CacheMissCount())

        // Every solve() is either a cache hit or a miss.
        assertEquals(
            statistics.getSqlZ3QueriesSeenCount(),
            statistics.getSqlZ3CacheHitCount() + statistics.getSqlZ3CacheMissCount()
        )
    }

    @Test
    fun testReExecutionTimes() {
        val statistics = Statistics()

        statistics.reportReExecutionTime(10)
        statistics.reportReExecutionTime(25)
        statistics.reportReExecutionTime(5)

        assertEquals(listOf(10L, 25L, 5L), statistics.getAllReExecutionTimesMs())
        assertEquals(40L, statistics.getSumOfReExecutionTimesMs())
    }

}
