package org.evomaster.core.problem.enterprise.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jExistingDataDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyExistingValueDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jDatabaseCommandsDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionResultsDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.database.neo4j.Neo4jDbAction
import org.evomaster.core.database.neo4j.Neo4jDbActionResult
import org.evomaster.core.database.neo4j.Neo4jNodeTemplate
import org.evomaster.core.database.neo4j.Neo4jPropertyGene
import org.evomaster.core.database.neo4j.Neo4jUniqueKeys
import org.evomaster.core.problem.rest.service.fitness.AbstractRestFitness
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.action.ActionResult
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnterpriseFitnessNeo4jTest {

    private val sent = slot<Neo4jDatabaseCommandsDto>()

    /** A fitness whose controller inserts every node except the ones whose name is in [failing]. */
    private fun fitness(vararg failing: String): AbstractRestFitness {
        val rc = mockk<RemoteController>()
        every { rc.executeNeo4jInsertions(capture(sent)) } answers {
            Neo4jInsertionResultsDto().also { results ->
                results.nodeExecutionResults = sent.captured.nodes
                    .map { node -> node.properties.none { it.propertyKey == "name" && it.value in failing } }
                    .toMutableList()
            }
        }
        val fitness = mockk<AbstractRestFitness>(relaxed = true)
        every { fitness.doNeo4jDbCalls(any(), any()) } answers { callOriginal() }
        every { fitness getProperty "rc" } propertyType RemoteController::class answers { rc }
        return fitness
    }

    private fun insertion(name: String) = Neo4jDbAction(
        listOf(Neo4jNodeTemplate(listOf("Player"), listOf(
            Neo4jPropertyGene("name", Neo4jPropertyTypeDto.STRING, StringGene("name", name))))),
        emptyList(),
        "MATCH (p:Player) RETURN p"
    )

    private fun existingData(vararg ids: String): List<Neo4jDbAction> {
        val key = Neo4jUniqueKeyDto().also { dto ->
            dto.label = "Player"
            dto.properties = mutableListOf("id")
            dto.existingValues = ids.map {
                Neo4jUniqueKeyExistingValueDto(listOf(Neo4jInsertionEntryDto("id", Neo4jPropertyTypeDto.STRING, it)))
            }.toMutableList()
        }
        val uniqueKeys = Neo4jUniqueKeys(Neo4jExistingDataDto().also { it.uniqueKeys = mutableListOf(key) })
        return uniqueKeys.existingDataFor(listOf("Player"))
    }

    /** The [actions] with the local ids an individual gives them, which their results refer to. */
    private fun withIds(actions: List<Neo4jDbAction>) =
        actions.onEachIndexed { i, action -> action.setLocalId("NEO4J_$i") }

    private fun inserted(results: List<ActionResult>) = results.map { (it as Neo4jDbActionResult).getInsertExecutionResult() }

    @Test
    fun theDataAlreadyInTheDatabaseIsNotSent() {
        val actions = withIds(existingData("a", "b") + listOf(insertion("ana"), insertion("bob")))
        val results = mutableListOf<ActionResult>()

        fitness().doNeo4jDbCalls(actions, results)

        assertEquals(2, sent.captured.nodes.size)
        assertTrue(sent.captured.nodes.all { node -> node.properties.any { it.propertyKey == "name" } })
    }

    @Test
    fun eachResultStaysWithItsActionWhenTheDataAlreadyInTheDatabaseIsSkipped() {
        val actions = withIds(existingData("a") + listOf(insertion("ana"), insertion("bob")))
        val results = mutableListOf<ActionResult>()

        fitness("ana").doNeo4jDbCalls(actions, results)

        assertEquals(actions.map { it.getLocalId() }, results.map { it.sourceLocalId })
        assertEquals(listOf(false, false, true), inserted(results))
    }

    @Test
    fun nothingIsSentWhenTheActionsOnlyRepresentExistingData() {
        val actions = withIds(existingData("a"))
        val results = mutableListOf<ActionResult>()

        assertTrue(fitness().doNeo4jDbCalls(actions, results))

        assertFalse(sent.isCaptured)
        assertEquals(1, results.size)
    }
}
