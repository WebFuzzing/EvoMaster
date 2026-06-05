package org.evomaster.core.problem.mcp.service

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.mcp.McpAction
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.problem.mcp.McpResourceReadAction
import org.evomaster.core.problem.mcp.McpToolCallAction
import org.evomaster.core.problem.mcp.client.HttpMcpClient
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.ObjectGene
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * Sampler for MCP blackbox testing.
 *
 * On initialization, connects to the MCP server, discovers
 * tools and resources, and builds an action cluster. Follows the pattern of GraphQLSampler
 * (blackbox init) and RPCSampler (dual cluster + adHoc individuals).
 */
class McpSampler : ApiWsSampler<McpIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(McpSampler::class.java)
    }

    private lateinit var mcpClient: HttpMcpClient

    /** Actions for MCP tool calls, keyed by "tool:<toolName>" */
    private val toolActionCluster: MutableMap<String, McpToolCallAction> = mutableMapOf()

    /** Actions for MCP resource reads, keyed by "resource:<uri>" or template key */
    private val resourceActionCluster: MutableMap<String, McpResourceReadAction> = mutableMapOf()

    /** Pre-built single-call individuals, drained first in smartSample() */
    private val adHocInitialIndividuals: MutableList<McpIndividual> = mutableListOf()

    @PostConstruct
    fun initialize() {
        val name = McpSampler::class.simpleName
        val url = config.bbTargetUrl
        log.debug("Initializing {}", name)


        mcpClient = HttpMcpClient(url)

        actionCluster.clear()
        toolActionCluster.clear()
        resourceActionCluster.clear()

        // MCP requires initialize handshake before any other call
        try {
            mcpClient.initialize()
        } catch (e: Exception) {
            throw SutProblemException(
                "Failed to initialize MCP session at '${config.bbTargetUrl}'. Cause: ${e.message}"
            )
        }

        // Discover tools
        val tools = try {
            mcpClient.listTools()
        } catch (e: Exception) {
            throw SutProblemException(
                "Failed to connect to MCP server at '${config.bbTargetUrl}'. " +
                "Make sure the server is running and the URL is correct. Cause: ${e.message}"
            )
        }
        for (tool in tools) {
            val inputGene = buildObjectGeneFromSchema("input", tool.inputSchema)
            val action = McpToolCallAction(
                toolName = tool.name,
                inputSchema = inputGene,
                description = tool.description
            )
            toolActionCluster[action.id] = action
            actionCluster[action.id] = action
        }

        // Discover static resources
        val resources = mcpClient.listResources()
        for (resource in resources) {
            val uri = StringGene("uri", resource.uri)
            val action = McpResourceReadAction(uri = uri, isTemplate = false)
            resourceActionCluster[action.id] = action
            actionCluster[action.id] = action
        }

        // Discover resource templates
        val templates = mcpClient.listResourceTemplates()
        for (template in templates) {
            val uri = StringGene("uri", template.uriTemplate)
            val action = McpResourceReadAction(uri = uri, isTemplate = true)
            // Use "template:" prefix to avoid collision with static resource keys
            val key = "template:${template.uriTemplate}"
            resourceActionCluster[key] = action
            actionCluster[key] = action
        }

        customizeAdHocInitialIndividuals()

        val toolQuantity = toolActionCluster.size
        val resourceQuantity = resourceActionCluster.size

        log.debug("Done initializing {} — {} tools, {} resources",
            name, toolQuantity, resourceQuantity)
    }

    // -------------------------------------------------------------------------
    // Sampling
    // -------------------------------------------------------------------------

    override fun sampleAtRandom(): McpIndividual {
        val allActions: List<McpAction> =
            toolActionCluster.values.toList() + resourceActionCluster.values.toList()

        if (allActions.isEmpty()) {
            // Edge case: no capabilities discovered — return empty individual
            val ind = McpIndividual(SampleType.RANDOM, mutableListOf())
            ind.doGlobalInitialize(searchGlobalState)
            return ind
        }

        val n = randomness.nextInt(1, getMaxTestSizeDuringSampler())
        val groups: MutableList<ActionComponent> = (0 until n).map {
            val action = randomness.choose(allActions).copy() as McpAction
            action.doInitialize(randomness)
            makeGroup(action)
        }.toMutableList()

        val ind = McpIndividual(SampleType.RANDOM, groups)
        ind.doGlobalInitialize(searchGlobalState)
        return ind
    }

    override fun smartSample(): McpIndividual {
        if (adHocInitialIndividuals.isNotEmpty()) {
            return adHocInitialIndividuals.removeAt(adHocInitialIndividuals.size - 1)
        }
        return sampleAtRandom()
    }

    override fun hasSpecialInitForSmartSampler(): Boolean = adHocInitialIndividuals.isNotEmpty()

    override fun initSeededTests(infoDto: SutInfoDto?) {
        // Not supported in Phase 3
    }

    // -------------------------------------------------------------------------
    // AdHoc individuals — one per discovered capability
    // -------------------------------------------------------------------------

    private fun customizeAdHocInitialIndividuals() {
        adHocInitialIndividuals.clear()

        for ((_, action) in toolActionCluster) {
            val copy = action.copy() as McpAction
            copy.doInitialize(randomness)
            val ind = McpIndividual(SampleType.RANDOM, mutableListOf(makeGroup(copy)))
            ind.doGlobalInitialize(searchGlobalState)
            adHocInitialIndividuals.add(ind)
        }

        for ((_, action) in resourceActionCluster) {
            val copy = action.copy() as McpAction
            copy.doInitialize(randomness)
            val ind = McpIndividual(SampleType.RANDOM, mutableListOf(makeGroup(copy)))
            ind.doGlobalInitialize(searchGlobalState)
            adHocInitialIndividuals.add(ind)
        }
    }

    /**
     * Wrap a single [McpAction] in an [EnterpriseActionGroup].
     * Uses the single-action convenience constructor.
     */
    private fun makeGroup(action: McpAction): EnterpriseActionGroup<McpAction> {
        return EnterpriseActionGroup(action)
    }

    // -------------------------------------------------------------------------
    // Gene building from JSON Schema
    // -------------------------------------------------------------------------

    /**
     * Recursively build a [Gene] from a JSON Schema node.
     *
     * Supported types: string, integer, number, boolean, object, array.
     * Unknown or missing types fall back to [StringGene].
     */
    internal fun buildGeneFromSchema(name: String, schema: Map<String, Any?>): Gene {
        val type = schema["type"] as? String

        return when (type) {
            "string" -> StringGene(name)
            "integer", "number" -> IntegerGene(name)
            "boolean" -> BooleanGene(name)
            "object" -> buildObjectGeneFromSchema(name, schema)
            "array" -> ArrayGene(name, StringGene("element"))
            else -> StringGene(name) // fallback for unknown/null types
        }
    }

    /**
     * Build an [ObjectGene] from a JSON Schema map.
     * If the schema has no "properties" key, returns an empty [ObjectGene].
     */
    @Suppress("UNCHECKED_CAST")
    internal fun buildObjectGeneFromSchema(name: String, schema: Map<String, Any?>): ObjectGene {
        val properties = schema["properties"] as? Map<String, Any?> ?: emptyMap()
        val fields = properties.entries.map { (propName, propSchema) ->
            val propSchemaMap = propSchema as? Map<String, Any?> ?: emptyMap()
            buildGeneFromSchema(propName, propSchemaMap)
        }
        return ObjectGene(name, fields)
    }

    // -------------------------------------------------------------------------
    // Expose client for fitness function
    // -------------------------------------------------------------------------

    fun getMcpClient(): HttpMcpClient = mcpClient
}
