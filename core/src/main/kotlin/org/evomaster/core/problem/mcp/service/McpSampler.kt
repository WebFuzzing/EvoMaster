package org.evomaster.core.problem.mcp.service

import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.core.problem.api.service.ApiWsSampler
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.problem.mcp.client.HttpMcpClient
import javax.annotation.PostConstruct

class McpSampler : ApiWsSampler<McpIndividual>() {

    private lateinit var mcpClient: HttpMcpClient

    @PostConstruct
    fun initialize() {
        mcpClient = HttpMcpClient(config.base)
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun sampleAtRandom(): McpIndividual {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun smartSample(): McpIndividual {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun hasSpecialInitForSmartSampler(): Boolean {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    override fun initSeededTests(infoDto: SutInfoDto?) {
        throw UnsupportedOperationException("MCP server analysis is not yet supported")
    }

    fun getMcpClient(): HttpMcpClient = mcpClient
}
