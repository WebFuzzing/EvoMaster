package org.evomaster.core.output

import org.evomaster.core.database.cassandra.CassandraColumn
import org.evomaster.core.database.cassandra.CassandraDbAction
import org.evomaster.core.database.cassandra.CassandraDbActionResult
import org.evomaster.core.search.action.EvaluatedCassandraDbAction
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CassandraWriterTest {

    private var counter = 0

    private fun makeEvaluated(
        keyspace: String = "ks",
        table: String = "users",
        columns: List<CassandraColumn> = listOf(CassandraColumn("name", "text")),
        genes: List<Gene> = listOf(StringGene("name", "Alice")),
        success: Boolean = true
    ): EvaluatedCassandraDbAction {
        val action = CassandraDbAction(keyspace, table, columns, genes)
        action.setLocalId("test-cassandra-action-${counter++}")
        val result = CassandraDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedCassandraDbAction(action, result)
    }

    private fun write(
        actions: List<EvaluatedCassandraDbAction>,
        format: OutputFormat = OutputFormat.KOTLIN_JUNIT_5,
        insertionVars: MutableList<Pair<String, String>> = mutableListOf(),
        skipFailure: Boolean = false,
        groupIndex: String = ""
    ): String {
        val lines = Lines(format)
        CassandraWriter.handleCassandraDbInitialization(format, actions, lines, groupIndex, insertionVars, skipFailure)
        return lines.toString()
    }

    @Test
    fun testEmptyListGeneratesNothing() {
        assertTrue(write(emptyList()).isBlank())
    }

    @Test
    fun testAllFailedWithSkipFailureGeneratesNothing() {
        assertTrue(write(listOf(makeEvaluated(success = false)), skipFailure = true).isBlank())
    }

    @Test
    fun testFailedInsertionIsKeptWhenNotSkipping() {
        assertTrue(write(listOf(makeEvaluated(success = false))).contains(".insertInto(\"ks\", \"users\")"))
    }

    @Test
    fun testKotlinOutput() {
        val output = write(listOf(makeEvaluated()))

        assertTrue(output.contains("val insertions_cassandra = cassandra()"))
        assertTrue(output.contains(".insertInto(\"ks\", \"users\")"))
        assertTrue(output.contains(".d(\"name\", \"'Alice'\")"))
        assertTrue(output.contains(".dtos()"))
        assertTrue(output.contains("val insertions_cassandra_result = controller.execInsertionsIntoCassandraDatabase(insertions_cassandra)"))
    }

    @Test
    fun testJavaOutput() {
        val output = write(listOf(makeEvaluated()), format = OutputFormat.JAVA_JUNIT_5)

        assertTrue(output.contains("List<CassandraInsertionDto> insertions_cassandra = cassandra()"))
        assertTrue(output.contains("CassandraInsertionResultsDto insertions_cassandra_result = controller.execInsertionsIntoCassandraDatabase(insertions_cassandra)"))
    }

    @Test
    fun testOneColumnPerGene() {
        val output = write(
            listOf(
                makeEvaluated(
                    columns = listOf(CassandraColumn("name", "text"), CassandraColumn("age", "int")),
                    genes = listOf(StringGene("name", "Alice"), IntegerGene("age", 42))
                )
            )
        )

        assertTrue(output.contains(".d(\"name\", \"'Alice'\")"))
        assertTrue(output.contains(".d(\"age\", \"42\")"))
    }

    @Test
    fun testSeveralActionsAreChained() {
        val output = write(listOf(makeEvaluated(), makeEvaluated(table = "events")))

        assertTrue(output.contains(".insertInto(\"ks\", \"users\")"))
        assertTrue(output.contains(".and().insertInto(\"ks\", \"events\")"))
    }

    /**
     * The CQL literal ends up inside a string literal of the generated test, so it has to be
     * escaped for the language that test is written in.
     */
    @Test
    fun testValueIsEscapedForTheGeneratedTest() {
        val output = write(
            listOf(makeEvaluated(genes = listOf(StringGene("name", "a\"b"))))
        )

        assertTrue(output.contains(".d(\"name\", \"'a\\\"b'\")"))
    }

    @Test
    fun testDollarIsEscapedInKotlinOnly() {
        val genes = listOf<Gene>(StringGene("name", "a\$b"))

        val kotlin = write(listOf(makeEvaluated(genes = genes)), format = OutputFormat.KOTLIN_JUNIT_5)
        assertTrue(kotlin.contains("\\$"))

        val java = write(listOf(makeEvaluated(genes = genes)), format = OutputFormat.JAVA_JUNIT_5)
        assertFalse(java.contains("\\$"))
    }

    @Test
    fun testInsertionVarIsRegisteredForFollowingGroups() {
        val insertionVars = mutableListOf<Pair<String, String>>()

        write(listOf(makeEvaluated()), insertionVars = insertionVars)

        assertTrue(insertionVars.contains("insertions_cassandra" to "insertions_cassandra_result"))
    }

    @Test
    fun testPreviousInsertionVarsArePassedOn() {
        val insertionVars = mutableListOf("insertions" to "insertionsresult")

        val output = write(listOf(makeEvaluated()), insertionVars = insertionVars, groupIndex = "1")

        assertTrue(output.contains("val insertions_cassandra1 = cassandra(insertions)"))
    }
}
