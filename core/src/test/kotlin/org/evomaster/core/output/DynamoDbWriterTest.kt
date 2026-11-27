package org.evomaster.core.output

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbDtoUtils
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto
import org.evomaster.core.database.dynamodb.DynamoDbAction
import org.evomaster.core.database.dynamodb.DynamoDbActionResult
import org.evomaster.core.database.dynamodb.DynamoDbAttributeGene
import org.evomaster.core.database.dynamodb.DynamoDbInsertBuilder
import org.evomaster.core.database.dynamodb.DynamoDbValueType
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.search.action.EvaluatedDynamoDbAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.numeric.BigDecimalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** Tests generated DynamoDB initialization code. */
class DynamoDbWriterTest {

    /** Creates an evaluated insertion for an Argentinian World Cup player. */
    private fun evaluatedWorldCupPlayer(): EvaluatedDynamoDbAction {
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene(
                    "country",
                    DynamoDbValueType.STRING,
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

    /** Documents and sets must use the DynamoDB JSON DSL. */
    @Test
    fun writesDocumentAndSetTemplatesWithDdbJson() {
        val document = DynamoDbDtoUtils.fromPlainValue("squad", mapOf("player" to "Lionel Messi"))
        val roles = DynamoDbDtoUtils.fromPlainValue("roles", setOf("forward", "captain"))
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene(
                    "squad",
                    DynamoDbValueType.fromDto(document.type),
                    DynamoDbInsertBuilder.buildDocumentGene("squad", document)
                ),
                DynamoDbAttributeGene(
                    "roles",
                    DynamoDbValueType.fromDto(roles.type),
                    DynamoDbInsertBuilder.buildDocumentGene("roles", roles)
                )
            )
        )
        action.setLocalId("world-cup-squad")
        val result = DynamoDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(true) }
        val lines = Lines(OutputFormat.KOTLIN_JUNIT_5)
        DynamoDbWriter.handleDynamoDbInitialization(
            OutputFormat.KOTLIN_JUNIT_5,
            listOf(EvaluatedDynamoDbAction(action, result)),
            lines,
            dynamoDbInsertionVars = mutableListOf(),
            skipFailure = false
        )
        val output = lines.toString()
        assertTrue(
            output.contains(".ddbJson(\"squad\", \"{\\\"M\\\":{\\\"player\\\":{\\\"S\\\":\\\"Lionel Messi\\\"}}}\")"),
            output
        )
        assertTrue(
            output.contains(".ddbJson(\"roles\", \"{\\\"SS\\\":[\\\"captain\\\",\\\"forward\\\"]}\")"),
            output
        )
    }

    /** Generated scalar, document, set, and null calls must compile and preserve their DynamoDB types and values. */
    @Test
    fun generatedValuesCompileAndRoundTripThroughTheDsl() {
        val playerName = "Lionel O'Brien \"Messi\"\n\\captain${'$'}10"
        val document = DynamoDbDtoUtils.fromPlainValue(
            "squad",
            mapOf(
                "players" to listOf("Argentina", null, true),
                "nestedSets" to listOf(setOf("forward", "captain"), setOf(BigDecimal("10"), BigDecimal("30.5"))),
                "emptyAwards" to emptyList<Any?>(),
                "score" to BigDecimal("10.50")
            )
        )
        val positions = DynamoDbDtoUtils.fromPlainValue("positions", setOf("forward", "captain"))
        val jerseyNumbers = DynamoDbDtoUtils.fromPlainValue("jerseyNumbers", setOf(BigDecimal("10"), BigDecimal("30.5")))
        val coach = DynamoDbDtoUtils.fromPlainValue("coach", null)
        val action = DynamoDbAction(
            "WorldCupPlayers",
            listOf(
                DynamoDbAttributeGene("player", DynamoDbValueType.STRING, StringGene("player", playerName)),
                DynamoDbAttributeGene("shirt", DynamoDbValueType.STRING, StringGene("shirt", "10")),
                DynamoDbAttributeGene("activeText", DynamoDbValueType.STRING, StringGene("activeText", "true")),
                DynamoDbAttributeGene("score", DynamoDbValueType.NUMBER, BigDecimalGene("score", BigDecimal("10.50"))),
                DynamoDbAttributeGene("captain", DynamoDbValueType.BOOLEAN, BooleanGene("captain", true)),
                DynamoDbAttributeGene("squad", DynamoDbValueType.fromDto(document.type), DynamoDbInsertBuilder.buildDocumentGene("squad", document)),
                DynamoDbAttributeGene("positions", DynamoDbValueType.fromDto(positions.type), DynamoDbInsertBuilder.buildDocumentGene("positions", positions)),
                DynamoDbAttributeGene("jerseyNumbers", DynamoDbValueType.fromDto(jerseyNumbers.type), DynamoDbInsertBuilder.buildDocumentGene("jerseyNumbers", jerseyNumbers)),
                DynamoDbAttributeGene("coach", DynamoDbValueType.fromDto(coach.type), DynamoDbInsertBuilder.buildDocumentGene("coach", coach))
            )
        )
        action.setLocalId("world-cup-round-trip")
        val result = DynamoDbActionResult(action.getLocalId()).also { it.setInsertExecutionResult(true) }

        // TODO: also compile and round-trip OutputFormat.JAVA_JUNIT_5 once CompilerForTestGenerated supports Java compilation.
        val format = OutputFormat.KOTLIN_JUNIT_5
        val lines = Lines(format)
        DynamoDbWriter.handleDynamoDbInitialization(
            format,
            listOf(EvaluatedDynamoDbAction(action, result)),
            lines,
            dynamoDbInsertionVars = mutableListOf(),
            skipFailure = false
        )
        val output = lines.toString()
        val prefix = output.substringBefore("val insertions_dynamodb_result")
        val className = "WorldCupKotlinInsertions"
        val code = """
            import org.evomaster.client.java.controller.SutHandler;
            import org.evomaster.client.java.controller.api.dto.database.operations.*;
            import org.evomaster.client.java.controller.dynamodb.dsl.DynamoDbDsl.dynamoDb
            class $className {
                fun setup(controller: SutHandler) { $output }
                fun build(): List<DynamoDbInsertionDto> { $prefix return insertions_dynamodb }
            }
        """.trimIndent()
        CompilerForTestGenerated.compile(format, code, className)
        val compiled = javaClass.classLoader.loadClass(className)
        val insertions = compiled.getMethod("build").invoke(compiled.getConstructor().newInstance()) as
            List<DynamoDbInsertionDto>
        val attributes = insertions.single().attributes.associateBy { it.attributeName }
        assertEquals(playerName, attributes.getValue("player").value)
        assertEquals("10", DynamoDbDtoUtils.toPlainValue(attributes.getValue("shirt")))
        assertEquals("true", DynamoDbDtoUtils.toPlainValue(attributes.getValue("activeText")))
        assertEquals(BigDecimal("10.50"), DynamoDbDtoUtils.toPlainValue(attributes.getValue("score")))
        assertEquals(true, DynamoDbDtoUtils.toPlainValue(attributes.getValue("captain")))
        assertEquals(document, attributes.getValue("squad"))
        assertEquals(DynamoDbDtoUtils.toPlainValue(document), DynamoDbDtoUtils.toPlainValue(attributes.getValue("squad")))
        assertEquals(positions, attributes.getValue("positions"))
        assertEquals(jerseyNumbers, attributes.getValue("jerseyNumbers"))
        assertEquals(coach, attributes.getValue("coach"))
    }
}
