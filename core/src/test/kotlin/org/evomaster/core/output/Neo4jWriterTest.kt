package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.database.neo4j.Neo4jDbAction
import org.evomaster.core.database.neo4j.Neo4jDbActionResult
import org.evomaster.core.database.neo4j.Neo4jEdgeTemplate
import org.evomaster.core.database.neo4j.Neo4jNodeTemplate
import org.evomaster.core.database.neo4j.Neo4jPropertyGene
import org.evomaster.core.search.action.EvaluatedNeo4jDbAction
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Neo4jWriterTest {

    private fun evaluatedPlayerWithUser(success: Boolean = true, localId: String = "neo4j-insertion"): EvaluatedNeo4jDbAction {
        val action = Neo4jDbAction(
            listOf(
                Neo4jNodeTemplate(listOf("Player"), emptyList()),
                Neo4jNodeTemplate(listOf("User"), listOf(Neo4jPropertyGene("username", Neo4jPropertyTypeDto.STRING, StringGene("username", "o'neil"))))
            ),
            listOf(Neo4jEdgeTemplate("HAS_USER", 0, 1, listOf(Neo4jPropertyGene("since", Neo4jPropertyTypeDto.INTEGER, LongGene("since", 2020L))))),
            "q"
        )
        action.setLocalId(localId)
        val result = Neo4jDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(success) }
        return EvaluatedNeo4jDbAction(action, result)
    }

    private fun write(format: OutputFormat, actions: List<EvaluatedNeo4jDbAction>, groupIndex: String = "",
                      vars: MutableList<Pair<String, String>> = mutableListOf(), skipFailure: Boolean = false): String {
        val lines = Lines(format)
        Neo4jWriter.handleNeo4jDbInitialization(format, actions, lines, groupIndex, vars, skipFailure)
        return lines.toString()
    }

    @Test
    fun generatesKotlinDslCallsWithQuotedStringsAndChainedIds() {
        val output = write(OutputFormat.KOTLIN_JUNIT_5, listOf(evaluatedPlayerWithUser()))

        assertTrue(output.contains("val insertions_neo4j = neo4j()"), output)
        assertTrue(output.contains(".createNode(1L, \"Player\")"), output)
        assertTrue(output.contains(".createNode(2L, \"User\")"), output)
        assertTrue(output.contains(".d(\"username\", \"'o''neil'\")"), output)
        assertTrue(output.contains(".createEdge(\"HAS_USER\", 1L, 2L)"), output)
        assertTrue(output.contains(".d(\"since\", \"2020\")"), output)
        assertTrue(output.contains(".dtos()"), output)
        assertTrue(output.contains("val insertions_neo4j_result = controller.execInsertionsIntoNeo4jDatabase(insertions_neo4j)"), output)
    }

    @Test
    fun generatesJavaDeclarations() {
        val output = write(OutputFormat.JAVA_JUNIT_5, listOf(evaluatedPlayerWithUser()), groupIndex = "_2")

        assertTrue(output.contains("Neo4jDatabaseCommandsDto insertions_neo4j_2 = neo4j()"), output)
        assertTrue(output.contains("Neo4jInsertionResultsDto insertions_neo4j_2_result = controller.execInsertionsIntoNeo4jDatabase(insertions_neo4j_2);"), output)
    }

    @Test
    fun idsKeepCountingAcrossActionsOfOneGroup() {
        val output = write(OutputFormat.KOTLIN_JUNIT_5,
            listOf(evaluatedPlayerWithUser(localId = "first"), evaluatedPlayerWithUser(localId = "second")))

        assertTrue(output.contains(".createNode(3L, \"Player\")"), output)
        assertTrue(output.contains(".createEdge(\"HAS_USER\", 3L, 4L)"), output)
    }

    @Test
    fun failedInsertionsAreSkippedOnRequestAndVariablesAreTracked() {
        val vars = mutableListOf<Pair<String, String>>()

        assertTrue(write(OutputFormat.KOTLIN_JUNIT_5, listOf(evaluatedPlayerWithUser(success = false)), vars = vars, skipFailure = true).isBlank())
        assertTrue(vars.isEmpty())

        write(OutputFormat.KOTLIN_JUNIT_5, listOf(evaluatedPlayerWithUser(success = false)), groupIndex = "_1", vars = vars, skipFailure = false)
        assertEquals(listOf("insertions_neo4j_1" to "insertions_neo4j_1_result"), vars)
    }
}
