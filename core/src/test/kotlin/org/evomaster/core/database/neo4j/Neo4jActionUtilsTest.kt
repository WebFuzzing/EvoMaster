package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jDbInfoDto
import org.evomaster.client.java.controller.api.dto.database.neo4j.Neo4jUniqueKeyDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rest.data.RestCallAction
import org.evomaster.core.problem.rest.data.RestIndividual
import org.evomaster.core.problem.rest.data.RestPath
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class Neo4jActionUtilsTest {

    private val randomness = Randomness().apply { updateSeed(42) }

    private fun key(label: String, vararg properties: String, existing: List<List<Neo4jInsertionEntryDto>> = emptyList()) =
        Neo4jUniqueKeyDto().also {
            it.label = label
            it.propertyKeys = properties.toMutableList()
            it.existingValues = existing.toMutableList()
        }

    private fun keys(vararg keys: Neo4jUniqueKeyDto) =
        Neo4jUniqueKeys(Neo4jDbInfoDto().also { it.uniqueKeys = keys.toMutableList() })

    private fun string(key: String, value: String) = Neo4jPropertyGene(key, Neo4jPropertyTypeDto.STRING, StringGene(key, value))

    private fun integer(key: String, value: Long) = Neo4jPropertyGene(key, Neo4jPropertyTypeDto.INTEGER, LongGene(key, value))

    private fun node(label: String, vararg properties: Neo4jPropertyGene) = Neo4jNodeTemplate(listOf(label), properties.toList())

    private fun action(uniqueKeys: Neo4jUniqueKeys?, vararg nodes: Neo4jNodeTemplate) =
        Neo4jDbAction(nodes.toList(), emptyList(), "MATCH (n) RETURN n", uniqueKeys)

    private fun value(action: Neo4jDbAction, node: Int = 0, property: String = "id"): String =
        action.nodes[node].properties.first { it.key == property }.valueAsText()

    @Test
    fun anIdRepeatedInTheSameTestIsChangedOnTheLaterInsertion() {
        val uniqueKeys = keys(key("Adopter", "id"))
        val first = action(uniqueKeys, node("Adopter", string("id", "gQNkuJUNO_nF"), string("name", "Ana")))
        val second = action(uniqueKeys, node("Adopter", string("id", "gQNkuJUNO_nF"), string("name", "Ana")))

        val broken = Neo4jActionUtils.repairUniqueKeys(listOf(first, second), randomness)

        assertTrue(broken.isEmpty())
        assertEquals("gQNkuJUNO_nF", value(first))
        assertNotEquals("gQNkuJUNO_nF", value(second))
        assertEquals("Ana", value(second, property = "name"))
    }

    @Test
    fun anIdAlreadyInTheDatabaseIsChanged() {
        val uniqueKeys = keys(key("Player", "id",
            existing = listOf(listOf(Neo4jInsertionEntryDto("id", Neo4jPropertyTypeDto.INTEGER, "7")))))
        val insertion = action(uniqueKeys, node("Player", integer("id", 7)))

        assertTrue(Neo4jActionUtils.repairUniqueKeys(listOf(insertion), randomness).isEmpty())
        assertNotEquals("7", value(insertion))
    }

    @Test
    fun theSameTextWithAnotherTypeIsNotARepetition() {
        val uniqueKeys = keys(key("Player", "id",
            existing = listOf(listOf(Neo4jInsertionEntryDto("id", Neo4jPropertyTypeDto.INTEGER, "7")))))
        val insertion = action(uniqueKeys, node("Player", string("id", "7")))

        Neo4jActionUtils.repairUniqueKeys(listOf(insertion), randomness)

        assertEquals("7", value(insertion))
    }

    @Test
    fun onlyNodesWithTheLabelOfTheKeyAreBound() {
        val uniqueKeys = keys(key("Adopter", "id"))
        val first = action(uniqueKeys, node("Pet", string("id", "x")))
        val second = action(uniqueKeys, node("Pet", string("id", "x")))

        Neo4jActionUtils.repairUniqueKeys(listOf(first, second), randomness)

        assertEquals("x", value(first))
        assertEquals("x", value(second))
    }

    @Test
    fun aCompositeKeyOnlyClashesOnAllItsProperties() {
        val uniqueKeys = keys(key("Match", "white", "black"))
        val a = action(uniqueKeys, node("Match", string("white", "w"), string("black", "b1")))
        val b = action(uniqueKeys, node("Match", string("white", "w"), string("black", "b2")))
        val c = action(uniqueKeys, node("Match", string("white", "w")))
        val d = action(uniqueKeys, node("Match", string("white", "w"), string("black", "b1")))

        Neo4jActionUtils.repairUniqueKeys(listOf(a, b, c, d), randomness)

        assertEquals(listOf("w", "b1"), listOf(value(a, property = "white"), value(a, property = "black")))
        assertEquals(listOf("w", "b2"), listOf(value(b, property = "white"), value(b, property = "black")))
        assertEquals("w", value(c, property = "white"))
        assertNotEquals(listOf("w", "b1"), listOf(value(d, property = "white"), value(d, property = "black")))
    }

    @Test
    fun twoNodesOfTheSameInsertionCannotShareAnId() {
        val uniqueKeys = keys(key("Person", "id"))
        val insertion = action(uniqueKeys, node("Person", string("id", "p")), node("Person", string("id", "p")))

        Neo4jActionUtils.repairUniqueKeys(listOf(insertion), randomness)

        assertEquals("p", value(insertion, node = 0))
        assertNotEquals("p", value(insertion, node = 1))
    }

    @Test
    fun anInsertionThatCannotBeRepairedIsReportedAndFreesItsValues() {
        val taken = listOf(true, false).map { listOf(Neo4jInsertionEntryDto("flag", Neo4jPropertyTypeDto.BOOLEAN, it.toString())) }
        val uniqueKeys = keys(key("Person", "id"), key("Flag", "flag", existing = taken))
        val hopeless = action(uniqueKeys,
            node("Person", string("id", "p")),
            node("Flag", Neo4jPropertyGene("flag", Neo4jPropertyTypeDto.BOOLEAN, BooleanGene("flag", true))))
        val later = action(uniqueKeys, node("Person", string("id", "p")))

        val broken = Neo4jActionUtils.repairUniqueKeys(listOf(hopeless, later), randomness)

        assertEquals(listOf(hopeless), broken)
        // the removed insertion no longer holds "p", so the later one keeps it
        assertEquals("p", value(later))
    }

    @Test
    fun insertionsWithoutKeysAreLeftAlone() {
        val first = action(null, node("Adopter", string("id", "x")))
        val second = action(null, node("Adopter", string("id", "x")))

        assertTrue(Neo4jActionUtils.repairUniqueKeys(listOf(first, second), randomness).isEmpty())
        assertEquals("x", value(second))
    }

    @Test
    fun keysWithoutLabelOrPropertiesAndIncompleteValuesAreDropped() {
        val uniqueKeys = keys(
            key("", "id"),
            key("Adopter"),
            key("Player", "id", existing = listOf(
                listOf(Neo4jInsertionEntryDto("id", Neo4jPropertyTypeDto.INTEGER, "1")),
                listOf(Neo4jInsertionEntryDto("id", null, "2")),
                emptyList()))
        )

        assertEquals(listOf(Neo4jUniqueKeys.Key("Player", listOf("id"))), uniqueKeys.keys)
        assertEquals(1, uniqueKeys.existing.size)
    }

    @Test
    fun repairingTheIndividualRemovesTheInsertionsThatCannotBeRepaired() {
        val taken = listOf(true, false).map { listOf(Neo4jInsertionEntryDto("flag", Neo4jPropertyTypeDto.BOOLEAN, it.toString())) }
        val uniqueKeys = keys(key("Adopter", "id"), key("Flag", "flag", existing = taken))
        val individual = RestIndividual(
            actions = mutableListOf(RestCallAction("get", HttpVerb.GET, RestPath("/adopters"), mutableListOf())),
            sampleType = SampleType.RANDOM
        )
        val first = action(uniqueKeys, node("Adopter", string("id", "x")))
        val second = action(uniqueKeys, node("Adopter", string("id", "x")))
        val hopeless = action(uniqueKeys, node("Flag", Neo4jPropertyGene("flag", Neo4jPropertyTypeDto.BOOLEAN, BooleanGene("flag", false))))
        individual.addInitializingActions(listOf(first, second, hopeless))
        individual.seeTopGenes().forEach(Gene::markAllAsInitialized)

        individual.repairInitializationActions(randomness)

        val kept = individual.seeNeo4jDbActions()
        assertEquals(2, kept.size)
        assertEquals(1, kept.count { value(it) == "x" })
        assertTrue(kept.none { it === hopeless })
    }
}
