package org.evomaster.core.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableCheckExpressionDto
import org.evomaster.dbconstraint.ast.SqlCondition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import org.evomaster.core.database.sql.solver.SmtLibGenerator

/**
 * Verifies that sharing a parse memo across [SmtLibGenerator] instances removes the repeated cost of
 * re-parsing schema-level CHECK constraints.
 *
 * A generator is constructed on every solver cache miss, so without the memo each query re-parses
 * every constraint in the schema. Since a schema does not change within a run, one parse per
 * expression is enough, and a real schema carries dozens of them — the enum constraints an ORM emits
 * for PostgreSQL run to thousands of characters each, see [CheckConstraintParsingCostTest].
 */
class CheckExpressionMemoisationTest {

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val query = CCJSqlParserUtil.parse(
        "SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.ID = 42"
    )

    /**
     * Builds a schema whose CHECK expressions are all distinct, so the memo holds one entry per
     * expression rather than collapsing them. That keeps the measured saving equal to the real one:
     * each expression goes from being parsed once per query to once per run.
     */
    private fun schema(checksPerTable: Int, expression: (Int) -> String): DbInfoDto {
        val dto = mapper.readValue(
            javaClass.getResourceAsStream("/solver/sample-schema.json")!!.bufferedReader().readText(),
            DbInfoDto::class.java
        )
        var distinct = 0
        dto.tables.forEach { t ->
            t.tableCheckExpressions.clear()
            repeat(checksPerTable) {
                t.tableCheckExpressions.add(
                    TableCheckExpressionDto().apply { sqlCheckExpression = expression(distinct++) }
                )
            }
        }
        return dto
    }

    /**
     * An ordinary constraint. The saving this test measures comes from how many expressions a schema
     * carries, not from any one of them being expensive, so a cheap and universally supported shape
     * keeps the measurement independent of which dialect quirks the parser currently handles.
     */
    private fun ordinaryCheck(seed: Int): String = "(LEVEL <= ${100 + seed} AND LEVEL >= $seed)"

    private fun generateRepeatedly(schema: DbInfoDto, times: Int, memo: MutableMap<String, SqlCondition?>?): Long {
        val start = System.nanoTime()
        repeat(times) { SmtLibGenerator(schema, 1, memo).generateSMT(query) }
        return (System.nanoTime() - start) / 1_000_000
    }

    @Test
    fun `memoised and unmemoised generation agree`() {
        val schema = schema(2) { i -> "(LEVEL <= ${100 + i})" }
        val memo = Collections.synchronizedMap(HashMap<String, SqlCondition?>())

        val withoutMemo = SmtLibGenerator(schema, 1).generateSMT(query).toString()
        val withMemo = SmtLibGenerator(schema, 1, memo).generateSMT(query).toString()
        val withWarmMemo = SmtLibGenerator(schema, 1, memo).generateSMT(query).toString()

        assertEquals(withoutMemo, withMemo)
        assertEquals(withoutMemo, withWarmMemo, "a warm memo must not change the generated formula")
    }

    /** Unparseable constraints are remembered as such, so they are attempted exactly once. */
    @Test
    fun `failed parses are memoised too`() {
        // Deliberately malformed rather than merely exotic, so the test does not depend on which
        // dialect shapes the parser happens to reject at any given time.
        val schema = schema(1) { "(status = = ${it}" }
        val memo = Collections.synchronizedMap(HashMap<String, SqlCondition?>())

        repeat(3) { SmtLibGenerator(schema, 1, memo).generateSMT(query) }

        assertEquals(schema.tables.size, memo.size, "expected one entry per distinct expression")
        assertTrue(memo.values.all { it == null }, "unparseable expressions should memoise as null")
    }

    /**
     * The measurement that motivates the change. Several generations over a schema carrying one
     * twenty ordinary constraints per table: without the memo every generation re-parses all of them,
     * with the memo only the first does. The saving therefore approaches the number of generations,
     * which in a real run is the number of distinct queries the search encounters.
     *
     * The assertion keeps a wide margin so it states the effect without being sensitive to machine
     * speed.
     */
    @Test
    fun `memoisation removes the repeated cost of parsing schema constraints`() {
        // 20 distinct expressions per table, so the schema carries a hundred of them: enough for the
        // repeated parsing to be measurable without relying on any single one being slow.
        val schema = schema(20) { ordinaryCheck(it) }
        val repetitions = 5

        // Warm up so class loading and JIT do not land on whichever arm runs first.
        generateRepeatedly(schema, 1, null)

        val unmemoised = generateRepeatedly(schema, repetitions, null)
        val memoised = generateRepeatedly(
            schema, repetitions, Collections.synchronizedMap(HashMap())
        )

        println(
            "\nGeneration of $repetitions formulas over a schema with realistic CHECK constraints" +
                "\n  without memo: $unmemoised ms" +
                "\n  with memo:    $memoised ms"
        )

        assertTrue(memoised * 3 < unmemoised) {
            "expected memoisation to cut the cost substantially, but got ${unmemoised}ms -> ${memoised}ms"
        }
    }
}
