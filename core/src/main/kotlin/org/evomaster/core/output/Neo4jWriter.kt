package org.evomaster.core.output

import org.apache.commons.text.StringEscapeUtils
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.database.neo4j.Neo4jDbAction
import org.evomaster.core.database.neo4j.Neo4jPropertyGene
import org.evomaster.core.search.action.EvaluatedNeo4jDbAction

/**
 * Generates the code in the test dealing with the insertion of data into Neo4j databases, as calls
 * to the controller's Neo4j DSL.
 */
object Neo4jWriter {

    /**
     * @param format the format of the tests to be generated
     * @param actions the evaluated Neo4j actions to generate
     * @param lines where the generated lines are saved
     * @param groupIndex suffix distinguishing this initialization group
     * @param neo4jInsertionVars previous Neo4j insertion variable names and their result variable names
     * @param skipFailure whether failed insertions should be omitted
     */
    fun handleNeo4jDbInitialization(
        format: OutputFormat,
        actions: List<EvaluatedNeo4jDbAction>,
        lines: Lines,
        groupIndex: String = "",
        neo4jInsertionVars: MutableList<Pair<String, String>>,
        skipFailure: Boolean
    ) {
        val selected = actions.filter { !skipFailure || it.neo4jResult.getInsertExecutionResult() }
        if (selected.isEmpty()) return

        val insertionVar = "insertions_neo4j${groupIndex}"
        val resultVar = "${insertionVar}_result"

        lines.add(if (format.isJava()) "Neo4jDatabaseCommandsDto $insertionVar = neo4j()" else "val $insertionVar = neo4j()")
        lines.indent()
        var nextId = 1L
        selected.forEach { evaluated ->
            val action = evaluated.neo4jAction
            val firstId = nextId
            action.nodes.forEach { node ->
                val labels = node.labels.joinToString("") { ", \"${escape(it, format)}\"" }
                lines.add(".createNode(${nextId}L$labels)")
                node.properties.forEach { lines.add(propertyCall(it, format)) }
                nextId++
            }
            action.edges.forEach { edge ->
                lines.add(".createEdge(\"${escape(edge.type, format)}\", ${firstId + edge.fromIndex}L, ${firstId + edge.toIndex}L)")
                edge.properties.forEach { lines.add(propertyCall(it, format)) }
            }
        }
        lines.add(".dtos()")
        lines.appendSemicolon()
        lines.deindent()

        val declaration = if (format.isJava()) "Neo4jInsertionResultsDto " else "val "
        lines.add("${declaration}${resultVar} = controller.execInsertionsIntoNeo4jDatabase($insertionVar)")
        lines.appendSemicolon()
        neo4jInsertionVars.add(insertionVar to resultVar)
    }

    /** The DSL reads strings enclosed in single quotes, with inner quotes doubled; numbers and booleans as they are. */
    private fun propertyCall(property: Neo4jPropertyGene, format: OutputFormat): String {
        val printable = when (property.type) {
            Neo4jPropertyTypeDto.STRING -> "'" + property.valueAsText().replace("'", "''") + "'"
            else -> property.valueAsText()
        }
        return ".d(\"${escape(property.key, format)}\", \"${escape(printable, format)}\")"
    }

    private fun escape(value: String, format: OutputFormat): String =
        StringEscapeUtils.escapeJava(value).let { if (format.isKotlin()) it.replace("$", "\\$") else it }
}
