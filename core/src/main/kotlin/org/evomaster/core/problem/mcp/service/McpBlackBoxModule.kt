package org.evomaster.core.problem.mcp.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.FlakinessDetector
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler

/**
 * Guice module wiring the MCP blackbox testing components.
 *
 * Mirrors [GraphQLBlackBoxModule] but for the MCP problem domain.
 * No TestCaseWriter is bound yet (MCP test output not implemented in Phase 4).
 */
class McpBlackBoxModule(
    val usingRemoteController: Boolean
) : AbstractModule() {

    override fun configure() {

        bind(object : TypeLiteral<EnterpriseSampler<McpIndividual>>() {})
            .to(McpSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<McpIndividual>>() {})
            .to(McpSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(McpSampler::class.java)
            .asEagerSingleton()

        bind(McpSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<McpIndividual>>() {})
            .to(McpBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(McpBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<McpIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
            .to(object : TypeLiteral<Archive<McpIndividual>>() {})

        bind(Archive::class.java)
            .to(object : TypeLiteral<Archive<McpIndividual>>() {})

        bind(object : TypeLiteral<Minimizer<McpIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>() {})
            .to(object : TypeLiteral<Minimizer<McpIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<McpIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<*>>() {})
            .to(object : TypeLiteral<FlakinessDetector<McpIndividual>>() {})
            .asEagerSingleton()

        if (usingRemoteController) {
            bind(RemoteController::class.java)
                .to(RemoteControllerImplementation::class.java)
                .asEagerSingleton()
        }

        // TestCaseWriter for MCP is not yet implemented.
        // When MCP test output is added, bind TestCaseWriter to an McpTestCaseWriter here.
    }
}
