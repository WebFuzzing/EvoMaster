package org.evomaster.core.database.neo4j

import org.evomaster.client.java.controller.api.dto.database.execution.Neo4jFailedQueryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jEdgeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jInsertionEntryDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jNodeInsertionDto
import org.evomaster.client.java.controller.api.dto.database.operations.Neo4jPropertyTypeDto
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.DoubleGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Builds evolvable insertion actions from the MATCH queries the graph did not satisfy. The controller
 * already digested each query into the nodes and relationships that would satisfy it; here every
 * property value becomes a gene seeded with the value the query compared against.
 */
object Neo4jInsertBuilder {

    private val log: Logger = LoggerFactory.getLogger(Neo4jInsertBuilder::class.java)

    /**
     * @param failedQueries failed queries reported by the controller
     * @param existingInsertionKeys keys of the insertions already in the individual
     * @param schema what the SUT's entities declare about each label; with it, a node also gets the
     *               properties the query did not fix, with free genes
     * @return one action per failed query not already represented by [existingInsertionKeys]
     */
    fun buildInsertActions(
        failedQueries: List<Neo4jFailedQueryDto>,
        existingInsertionKeys: Set<String>,
        schema: Neo4jSchema? = null
    ): List<Neo4jDbAction> = failedQueries
        .mapNotNull { toActionOrNull(it, schema) }
        .filterNot { it.insertionKey() in existingInsertionKeys }
        .distinctBy { it.insertionKey() }

    /** Converts one failed query into an action, or returns null when it cannot be inserted as reported. */
    private fun toActionOrNull(query: Neo4jFailedQueryDto, schema: Neo4jSchema?): Neo4jDbAction? {
        val nodes = query.nodes ?: emptyList()
        if (nodes.isEmpty()) return null

        val indexById = nodes.mapIndexed { index, node -> node.id to index }.toMap()
        val nodeTemplates = nodes.map { node ->
            val labels = node.labels ?: emptyList()
            val fromQuery = toPropertyGenes(node.properties) ?: return null
            Neo4jNodeTemplate(labels, fromQuery + fromSchema(labels, fromQuery, schema))
        }
        val edgeTemplates = (query.edges ?: emptyList()).map { edge ->
            toEdgeOrNull(edge, indexById) ?: return null
        }
        return Neo4jDbAction(nodeTemplates, edgeTemplates, query.query ?: "")
    }

    private fun toEdgeOrNull(edge: Neo4jEdgeInsertionDto, indexById: Map<Long?, Int>): Neo4jEdgeTemplate? {
        val type = edge.type
        val from = indexById[edge.fromNodeId]
        val to = indexById[edge.toNodeId]
        if (type.isNullOrBlank() || from == null || to == null) {
            LoggingUtil.uniqueWarn(log, "Neo4j failed query with a relationship that cannot be inserted: $edge")
            return null
        }
        return Neo4jEdgeTemplate(type, from, to, toPropertyGenes(edge.properties) ?: return null)
    }

    /** @return the genes for the properties, or null when one of them holds a value of the wrong type */
    /**
     * Genes for the properties the schema declares for these labels and the query did not fix.
     */
    private fun fromSchema(labels: List<String>, fromQuery: List<Neo4jPropertyGene>, schema: Neo4jSchema?): List<Neo4jPropertyGene> {
        if (schema == null) return emptyList()
        val fixed = fromQuery.map { it.key }.toSet()
        return schema.propertiesFor(labels)
            .filter { it.name !in fixed && it.type != null }
            .map { p ->
                val gene = when (p.type!!) {
                    Neo4jPropertyTypeDto.STRING ->
                        if (p.enumValues.isNullOrEmpty()) StringGene(p.name) else EnumGene(p.name, p.enumValues)
                    Neo4jPropertyTypeDto.INTEGER -> LongGene(p.name, min = p.minValue, max = p.maxValue)
                    Neo4jPropertyTypeDto.FLOAT -> DoubleGene(p.name)
                    Neo4jPropertyTypeDto.BOOLEAN -> BooleanGene(p.name)
                }
                Neo4jPropertyGene(p.name, p.type, gene, fromQuery = false)
            }
    }

    private fun toPropertyGenes(properties: List<Neo4jInsertionEntryDto>?): List<Neo4jPropertyGene>? =
        (properties ?: emptyList()).map { toPropertyGeneOrNull(it) ?: return null }

    private fun toPropertyGeneOrNull(entry: Neo4jInsertionEntryDto): Neo4jPropertyGene? {
        val key = entry.propertyKey ?: return null
        val type = entry.type ?: return null
        val value = entry.value ?: return null
        val gene = when (type) {
            Neo4jPropertyTypeDto.STRING -> StringGene(key, value)
            Neo4jPropertyTypeDto.INTEGER -> LongGene(key, value.toLongOrNull() ?: return null)
            Neo4jPropertyTypeDto.FLOAT -> DoubleGene(key, value.toDoubleOrNull() ?: return null)
            Neo4jPropertyTypeDto.BOOLEAN -> BooleanGene(key, value.toBoolean())
        }
        return Neo4jPropertyGene(key, type, gene)
    }
}
