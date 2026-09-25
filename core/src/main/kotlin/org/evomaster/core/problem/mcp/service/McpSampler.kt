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
import org.evomaster.core.problem.mcp.McpUriParam
import org.evomaster.core.problem.mcp.builder.McpActionBuilder
import org.evomaster.core.problem.mcp.client.HttpMcpClient
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.action.ActionComponent
import org.evomaster.core.search.gene.string.StringGene
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct

/**
 * Sampler for MCP blackbox testing.
 *
 * On initialization ([initialize]), connects to the MCP server, performs the MCP handshake
 * and discovers capabilities:
 * - **Tools** (`tools/list`) → one [McpToolCallAction] per tool.
 * - **Static resources** (`resources/list`) → one [McpResourceReadAction] per URI.
 * - **Template resources** (`resources/templates/list`) → one [McpResourceReadAction] per template.
 */
class McpSampler : ApiWsSampler<McpIndividual>() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(McpSampler::class.java)
    }

    private lateinit var mcpClient: HttpMcpClient

    /** Actions for MCP tool calls, keyed by "tool:<toolName>" */
    private val toolActionCluster: MutableMap<String, McpToolCallAction> = mutableMapOf()

    /** Declared output JSON Schema per tool name, as returned by `tools/list` (null if the tool declares none) */
    private val outputSchemas: MutableMap<String, Map<String, Any?>?> = mutableMapOf()

    /** Actions for MCP resource reads, keyed by "resource:<uri>" or template key */
    private val resourceActionCluster: MutableMap<String, McpResourceReadAction> = mutableMapOf()

    /** Pre-built single-call individuals, drained first in smartSample() */
    private val adHocInitialIndividuals: MutableList<McpIndividual> = mutableListOf()

    /** Builds the tool actions cluster as part of the initialization process */
    private fun discoverTools() {
        val tools = mcpClient.listTools()
        val options = RestActionBuilderV3.Options(config)
        val messages = McpActionBuilder.addActionsFromToolList(tools, toolActionCluster, options)
        messages.forEach { log.warn(it) }
        toolActionCluster.values.forEach { actionCluster[it.id] = it }
    }

    /** Builds the resource actions cluster as part of the initialization process */
    private fun discoverResources() {
        val resources = mcpClient.listResources()
        for (resource in resources) {
            val action = McpResourceReadAction(uriTemplate = resource.uri, uriParams = emptyList(), isTemplate = false)
            resourceActionCluster[action.id] = action
            actionCluster[action.id] = action
        }
    }

    /** Add templatized resources tho the resource actions cluster as part of the initialization process */
    private fun discoverResourceTemplates() {
        val templates = mcpClient.listResourceTemplates()
        for (template in templates) {
            val paramNames = Regex("""\{(\w+)}""").findAll(template.uriTemplate).map { it.groupValues[1] }.toList()
            val uriParams = paramNames.map { McpUriParam(it, StringGene(it)) }
            val action = McpResourceReadAction(uriTemplate = template.uriTemplate, uriParams = uriParams, isTemplate = true)
            // Use "template:" prefix to avoid collision with static resource keys
            val key = "template:${template.uriTemplate}"
            resourceActionCluster[key] = action
            actionCluster[key] = action
        }
    }

    @PostConstruct
    fun initialize() {
        val name = McpSampler::class.simpleName
        val url = config.base
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
                "Failed to initialize MCP session at '${config.base}'. Cause: ${e.message}"
            )
        }

        // Discover server capabilities
        discoverTools()
        discoverResources()
        discoverResourceTemplates()

        // Builds the initial individuals
        customizeAdHocInitialIndividuals()
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

        // Build a random-sized test by picking N random actions, each wrapped in its own group
        val numberOfActions = randomness.nextInt(1, getMaxTestSizeDuringSampler())
        val groups: MutableList<ActionComponent> = (0 until numberOfActions).map {
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
        throw UnsupportedOperationException("MCP seeded testing is not yet supported")
    }

    // -------------------------------------------------------------------------
    // AdHoc individuals — one per discovered capability
    // -------------------------------------------------------------------------

    private fun customizeAdHocInitialIndividuals() {
        adHocInitialIndividuals.clear()

        val mcpActions = toolActionCluster.values + resourceActionCluster.values
        for (action in mcpActions) {
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
    // Expose client for fitness function
    // -------------------------------------------------------------------------

    fun getMcpClient(): HttpMcpClient = mcpClient
}
