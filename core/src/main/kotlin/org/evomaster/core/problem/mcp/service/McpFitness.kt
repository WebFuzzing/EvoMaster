package org.evomaster.core.problem.mcp.service

import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.search.service.FitnessFunction

/**
 * Abstract fitness function base for MCP testing.
 * Concrete subclasses provide the actual coverage calculation strategy
 * (blackbox, whitebox, etc.).
 */
abstract class McpFitness : FitnessFunction<McpIndividual>()
