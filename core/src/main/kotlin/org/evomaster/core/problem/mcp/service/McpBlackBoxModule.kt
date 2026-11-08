package org.evomaster.core.problem.mcp.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.AlgorithmRegistry
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.NoTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.mcp.McpIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.FlakinessDetector
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler

class McpBlackBoxModule(
    val usingRemoteController: Boolean
) : EnterpriseModule() {

    override fun configure() {
        super.configure()
        binder().requireExplicitBindings()

        if (usingRemoteController) {
            bindRemoteController()
        }

        AlgorithmRegistry.keys(EMConfig.ProblemType.MCP)
            .forEach { bind(it) }

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

        bind(TestCaseWriter::class.java)
            .to(NoTestCaseWriter::class.java)
            .asEagerSingleton()
    }
}
