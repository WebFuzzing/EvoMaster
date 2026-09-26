package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionKeyBuilder
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.search.action.Action
import org.evomaster.core.search.action.EnvironmentAction
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene

/**
 * A property of a node or relationship to insert.
 *
 * @property key name of the property
 * @property type type the property is stored with
 * @property gene evolvable value of the property
 */
data class Neo4jPropertyGene(
    val key: String,
    val type: Neo4jPropertyTypeDto,
    val gene: Gene
) {
    /** The value in the text form the controller reads, according to [type]. */
    fun valueAsText(): String = when (type) {
        Neo4jPropertyTypeDto.STRING -> (gene as StringGene).value
        Neo4jPropertyTypeDto.INTEGER -> (gene as LongGene).value.toString()
        Neo4jPropertyTypeDto.FLOAT -> (gene as DoubleGene).value.toString()
        Neo4jPropertyTypeDto.BOOLEAN -> (gene as BooleanGene).value.toString()
    }

    fun copy(): Neo4jPropertyGene = Neo4jPropertyGene(key, type, gene.copy())
}

/**
 * A node to insert.
 *
 * @property labels labels of the node
 * @property properties properties of the node
 */
data class Neo4jNodeTemplate(
    val labels: List<String>,
    val properties: List<Neo4jPropertyGene>
) {
    fun copy(): Neo4jNodeTemplate = Neo4jNodeTemplate(labels, properties.map { it.copy() })
}

/**
 * A relationship to insert between two nodes of the same action.
 *
 * @property type type of the relationship
 * @property fromIndex index in [Neo4jDbAction.nodes] of the node it starts from
 * @property toIndex index in [Neo4jDbAction.nodes] of the node it points to
 * @property properties properties of the relationship
 */
data class Neo4jEdgeTemplate(
    val type: String,
    val fromIndex: Int,
    val toIndex: Int,
    val properties: List<Neo4jPropertyGene>
) {
    fun copy(): Neo4jEdgeTemplate = Neo4jEdgeTemplate(type, fromIndex, toIndex, properties.map { it.copy() })
}

/**
 * An initialization action that inserts the nodes and relationships a MATCH query looked for and did
 * not find. They go together in one action because a relationship only makes sense with its two
 * endpoints, and the query needs all of them at once.
 *
 * @property nodes nodes to insert
 * @property edges relationships to insert, referring to [nodes] by index
 * @property query the Cypher text of the query this action was inferred from
 */
class Neo4jDbAction(
    val nodes: List<Neo4jNodeTemplate>,
    val edges: List<Neo4jEdgeTemplate>,
    val query: String
) : EnvironmentAction(listOf()) {

    init {
        addChildren(allProperties().map { it.gene })
    }

    private fun allProperties(): List<Neo4jPropertyGene> =
        nodes.flatMap { it.properties } + edges.flatMap { it.properties }

    override fun seeTopGenes(): List<Gene> = allProperties().map { it.gene }

    override fun copyContent(): Action = Neo4jDbAction(nodes.map { it.copy() }, edges.map { it.copy() }, query)

    override fun getName(): String =
        "Neo4j_INSERT_" + nodes.joinToString("_") { if (it.labels.isEmpty()) "node" else it.labels.joinToString(":") }

    override fun getActionGroupKey(): String = Neo4jDbAction::class.java.name

    /** Stable key used to avoid adding the same inferred insertion twice. */
    fun insertionKey(): String {
        val nodeDtos = nodes.mapIndexed { index, node -> toNodeDto(index.toLong(), node) }
        val edgeDtos = edges.map { toEdgeDto(it, 0) }
        return Neo4jInsertionKeyBuilder.fromCommands(nodeDtos, edgeDtos)
    }

    /**
     * @param id the id the node gets in the batch it is sent in
     */
    fun toNodeDto(id: Long, node: Neo4jNodeTemplate): Neo4jNodeInsertionDto =
        Neo4jNodeInsertionDto().also { dto ->
            dto.id = id
            dto.labels = node.labels.toMutableList()
            dto.properties = node.properties.map { toEntryDto(it) }.toMutableList()
        }

    /**
     * @param firstNodeId the id of the first node of this action in the batch it is sent in
     */
    fun toEdgeDto(edge: Neo4jEdgeTemplate, firstNodeId: Long): Neo4jEdgeInsertionDto =
        Neo4jEdgeInsertionDto().also { dto ->
            dto.type = edge.type
            dto.fromNodeId = firstNodeId + edge.fromIndex
            dto.toNodeId = firstNodeId + edge.toIndex
            dto.properties = edge.properties.map { toEntryDto(it) }.toMutableList()
        }

    private fun toEntryDto(property: Neo4jPropertyGene): Neo4jInsertionEntryDto =
        Neo4jInsertionEntryDto(property.key, property.type, property.valueAsText())
}
