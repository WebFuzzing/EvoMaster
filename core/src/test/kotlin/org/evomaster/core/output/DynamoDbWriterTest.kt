package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto
import org.evomaster.core.database.dynamodb.DynamoDbAction
import org.evomaster.core.database.dynamodb.DynamoDbActionResult
import org.evomaster.core.database.dynamodb.DynamoDbAttributeGene
import org.evomaster.core.search.action.EvaluatedDynamoDbAction
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests generated DynamoDB initialization code. */
class DynamoDbWriterTest {

    /** Creates an evaluated insertion for an Argentinian World Cup player. */
    private fun evaluatedWorldCupPlayer(): EvaluatedDynamoDbAction {
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene(
                    "country",
                    DynamoDbScalarTypeDto.STRING,
                    StringGene("country", "Argentina")
                )
            )
        )
        action.setLocalId("world-cup-player-insertion")
        val result = DynamoDbActionResult(action.getLocalId()).also {
            it.setInsertExecutionResult(true)
        }
        return EvaluatedDynamoDbAction(action, result)
    }

    @Test
    fun groupIndexDistinguishesGeneratedVariables() {
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)

        DynamoDbWriter.handleDynamoDbInitialization(
            format = OutputFormat.KOTLIN_JUNIT_5,
            actions = listOf(evaluatedWorldCupPlayer()),
            lines = lines,
            groupIndex = "_2",
            dynamoDbInsertionVars = mutableListOf(),
            skipFailure = false
        )

        val output = lines.toString()
        assertTrue(output.contains("val insertions_dynamodb_2 = dynamoDb()"))
        assertTrue(
            output.contains(
                "val insertions_dynamodb_2_result = " +
                    "controller.execInsertionsIntoDynamoDb(insertions_dynamodb_2)"
            )
        )
    }

    @Test
    fun defaultGroupIndexPreservesGeneratedVariables() {
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)

        DynamoDbWriter.handleDynamoDbInitialization(
            format = OutputFormat.KOTLIN_JUNIT_5,
            actions = listOf(evaluatedWorldCupPlayer()),
            lines = lines,
            dynamoDbInsertionVars = mutableListOf(),
            skipFailure = false
        )

        val output = lines.toString()
        assertTrue(output.contains("val insertions_dynamodb = dynamoDb()"))
        assertTrue(
            output.contains(
                "val insertions_dynamodb_result = " +
                    "controller.execInsertionsIntoDynamoDb(insertions_dynamodb)"
            )
        )
    }

    @Test
    fun previousInsertionVariablesAreTracked() {
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)
        val insertionVars = mutableListOf("insertions_dynamodb_1" to "insertions_dynamodb_1_result")

        DynamoDbWriter.handleDynamoDbInitialization(
            format = OutputFormat.KOTLIN_JUNIT_5,
            actions = listOf(evaluatedWorldCupPlayer()),
            lines = lines,
            groupIndex = "_2",
            dynamoDbInsertionVars = insertionVars,
            skipFailure = false
        )

        assertTrue(lines.toString().contains("val insertions_dynamodb_2 = dynamoDb(insertions_dynamodb_1)"))
        assertEquals(
            listOf(
                "insertions_dynamodb_1" to "insertions_dynamodb_1_result",
                "insertions_dynamodb_2" to "insertions_dynamodb_2_result"
            ),
            insertionVars
        )
    }
}
