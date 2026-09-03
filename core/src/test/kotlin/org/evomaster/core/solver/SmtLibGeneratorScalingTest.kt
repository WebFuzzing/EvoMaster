package org.evomaster.core.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableCheckExpressionDto
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import org.evomaster.core.database.sql.solver.SmtLibGenerator

/**
 * Measures how the cost of [SmtLibGenerator.generateSMT] scales with the size of the *schema*,
 * holding the query fixed.
 *
 * Motivation: an application with a large relational schema was observed spending nearly its whole
 * search budget inside SMT-LIB generation — hundreds of seconds per query — against roughly a
 * hundred milliseconds of actual Z3 solving. The obvious explanation is that generation is driven by
 * the size of the schema, since `generateSMT` performs seven steps of which five do not depend on
 * the query at all: `appendTableDefinitions`, `appendTableConstraints`, `appendKeyConstraints`,
 * `appendTimestampConstraints` and `appendBooleanConstraints` all iterate over every table. A fresh
 * [SmtLibGenerator] is built on each cache miss, so that work is repeated per query.
 *
 * These measurements test that explanation, and refute it: generation is linear and cheap, so schema
 * size cannot account for such a cost. See [CheckConstraintParsingCostTest] for what does.
 */
class SmtLibGeneratorScalingTest {

    companion object {
        private const val SCHEMA = "/solver/sample-schema.json"
        private const val NUMBER_OF_ROWS = 1

        /** A query touching a single table, so its own translation cost stays constant across sizes. */
        private const val QUERY =
            "SELECT label0_.ID AS ID1_2_ FROM LABEL label0_ WHERE label0_.ID = 42"
    }

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun baseSchema(): DbInfoDto =
        mapper.readValue(
            javaClass.getResourceAsStream(SCHEMA)!!.bufferedReader().readText(),
            DbInfoDto::class.java
        )

    /**
     * Grows the schema by replicating its tables under fresh names, so that column types, keys and
     * constraints keep a realistic mix. The query still targets one of the original tables.
     */
    private fun schemaWithTables(target: Int, keepForeignKeys: Boolean = false): DbInfoDto {
        val base = baseSchema()
        val originals = base.tables.toList()
        var copy = 0
        while (base.tables.size < target) {
            copy++
            for (t in originals) {
                if (base.tables.size >= target) break
                val clone = mapper.readValue(mapper.writeValueAsString(t), TableDto::class.java)
                val newName = "${t.id.name}_C$copy"
                clone.id.name = newName
                clone.columns.forEach { it.table = newName }
                if (!keepForeignKeys) clone.foreignKeys?.clear()
                base.tables.add(clone)
            }
        }
        return base
    }

    private fun generate(schema: DbInfoDto): Pair<Long, Int> {
        val statement = CCJSqlParserUtil.parse(QUERY)
        val start = System.nanoTime()
        val generator = SmtLibGenerator(schema, NUMBER_OF_ROWS)
        val smt = generator.generateSMT(statement)
        val ms = (System.nanoTime() - start) / 1_000_000
        return ms to smt.toString().toByteArray(StandardCharsets.UTF_8).size
    }

    /**
     * The generated formula must grow with the schema, not with the query. This is the property that
     * makes the cost schema-driven, and it is what a preamble cache would let us pay only once.
     */
    @Test
    fun `formula size is driven by the schema, not the query`() {
        val small = generate(schemaWithTables(14)).second
        val large = generate(schemaWithTables(224)).second

        assertTrue(large > small * 8) {
            "expected the formula to grow roughly with the schema: 14 tables -> $small bytes, " +
                "224 tables -> $large bytes"
        }
    }

    /**
     * Reports the scaling curve. Timings are not asserted on — they are machine dependent and would
     * make the test flaky — but the output documents the effect, and the byte counts are asserted.
     */
    @Test
    fun `report generation cost against schema size`() {
        val sizes = listOf(14, 28, 56, 112, 224, 448)
        val report = StringBuilder("\nSMT-LIB generation cost vs. schema size (query fixed)\n")
        report.append(String.format("%8s %10s %12s %14s%n", "tables", "ms", "bytes", "bytes/table"))

        var previousBytes = 0
        for (n in sizes) {
            val schema = schemaWithTables(n)
            generate(schema) // warm up JIT so the first row is not an outlier
            val (ms, bytes) = generate(schema)
            report.append(String.format("%8d %10d %12d %14d%n", n, ms, bytes, bytes / n))
            assertTrue(bytes > previousBytes) { "formula did not grow from ${previousBytes} at $n tables" }
            previousBytes = bytes
        }
        println(report)
    }

    /**
     * Same growth, but keeping the foreign keys on every cloned table instead of dropping them.
     *
     * This isolates the foreign-key path: `appendKeyConstraints` resolves each foreign key by
     * scanning the referenced table's columns, so the total work is proportional to
     * (foreign keys) x (columns of the referenced table). If generation is superlinear anywhere,
     * this is the most likely place, and it would explain a schema-size blow-up that the plain
     * table-count scaling does not.
     */
    @Test
    fun `report generation cost with foreign keys retained`() {
        val sizes = listOf(14, 28, 56, 112, 224)
        val report = StringBuilder("\nSMT-LIB generation cost with foreign keys retained\n")
        report.append(String.format("%8s %10s %12s %10s%n", "tables", "ms", "bytes", "FKs"))

        for (n in sizes) {
            val schema = schemaWithTables(n, keepForeignKeys = true)
            val fks = schema.tables.sumOf { it.foreignKeys?.size ?: 0 }
            generate(schema)
            val (ms, bytes) = generate(schema)
            report.append(String.format("%8d %10d %12d %10d%n", n, ms, bytes, fks))
        }
        println(report)
    }

    /**
     * Isolates the CHECK-constraint path.
     *
     * `appendTableConstraints` runs every table check expression through [JSqlConditionParser] on
     * every call — that is, once per table per query, with no caching of the parse. The corpus
     * schema has only two such expressions, but an ORM on PostgreSQL emits one per enum column, so
     * a large schema can carry hundreds. This is the remaining candidate for a cost that the
     * plain table-count and foreign-key scaling do not explain.
     */
    @Test
    fun `report generation cost against number of CHECK constraints`() {
        val report = StringBuilder("\nSMT-LIB generation cost vs. CHECK constraints (56 tables)\n")
        report.append(String.format("%10s %10s %12s%n", "checks", "ms", "bytes"))

        for (perTable in listOf(0, 1, 2, 4, 8, 16)) {
            val schema = schemaWithTables(56)
            schema.tables.forEach { t ->
                t.tableCheckExpressions.clear()
                val numeric = t.columns.firstOrNull { it.type.uppercase() in setOf("INTEGER", "BIGINT") }
                if (numeric != null) {
                    repeat(perTable) { i ->
                        t.tableCheckExpressions.add(
                            TableCheckExpressionDto().apply {
                                sqlCheckExpression = "(${numeric.name} <= ${100 + i})"
                            }
                        )
                    }
                }
            }
            val total = schema.tables.sumOf { it.tableCheckExpressions.size }
            generate(schema)
            val (ms, bytes) = generate(schema)
            report.append(String.format("%10d %10d %12d%n", total, ms, bytes))
        }
        println(report)
    }

    /**
     * The schema-invariant part dominates. Two different queries over the same schema produce
     * formulas of near-identical size, because both carry the full preamble. That is the redundancy
     * a preamble cache removes.
     */
    @Test
    fun `two different queries carry the same preamble`() {
        val schema = schemaWithTables(224)

        val a = SmtLibGenerator(schema, NUMBER_OF_ROWS)
            .generateSMT(CCJSqlParserUtil.parse(QUERY))
            .toString().toByteArray(StandardCharsets.UTF_8).size

        val b = SmtLibGenerator(schema, NUMBER_OF_ROWS)
            .generateSMT(CCJSqlParserUtil.parse("SELECT account0_.ID AS ID1_0_ FROM ACCOUNT account0_ WHERE account0_.ID = 7"))
            .toString().toByteArray(StandardCharsets.UTF_8).size

        val difference = Math.abs(a - b).toDouble() / maxOf(a, b)
        assertTrue(difference < 0.01) {
            "expected the query-specific part to be marginal, but the two formulas differ by " +
                "${"%.1f".format(difference * 100)}% ($a vs $b bytes)"
        }
    }
}
