package org.evomaster.core.problem.rpc.service

import com.google.inject.TypeLiteral
import org.evomaster.core.AlgorithmRegistry
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.RPCTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.FlakinessDetector
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * created by manzhang on 2021/11/26
 */
class RPCModule : EnterpriseModule(){

    override fun configure() {
        super.configure()
        binder().requireExplicitBindings()

        bindRemoteController()

        AlgorithmRegistry.keys(EMConfig.ProblemType.RPC)
            .forEach { bind(it) }

        bind(object : TypeLiteral<EnterpriseSampler<RPCIndividual>>() {})
            .to(RPCSampler::class.java)
        bind(object : TypeLiteral<Sampler<RPCIndividual>>() {})
            .to(RPCSampler::class.java)
        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(RPCSampler::class.java)
        bind(RPCSampler::class.java)
                .asEagerSingleton()

        bind(RPCEndpointsHandler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<RPCIndividual>>(){})
            .asEagerSingleton()
        bind(object : TypeLiteral<Minimizer<*>>(){})
            .to(object : TypeLiteral<Minimizer<RPCIndividual>>(){})

        bind(object : TypeLiteral<FitnessFunction<RPCIndividual>>() {})
                .to(RPCFitness::class.java)
        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(RPCFitness::class.java)
        bind(RPCFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<RPCIndividual>>(){})
            .asEagerSingleton()
        bind(object : TypeLiteral<FlakinessDetector<*>>(){})
            .to(object : TypeLiteral<FlakinessDetector<RPCIndividual>>(){})


        bind(object : TypeLiteral<Archive<RPCIndividual>>() {})
                .asEagerSingleton()
        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<RPCIndividual>>() {})
        bind(Archive::class.java)
            .to(object : TypeLiteral<Archive<RPCIndividual>>() {})

        bind(object : TypeLiteral<Mutator<RPCIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<RPCIndividual>>(){})
        bind(object : TypeLiteral<StandardMutator<RPCIndividual>>(){})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(RPCStructureMutator::class.java)
        bind(RPCStructureMutator::class.java)
            .asEagerSingleton()

        bind(TestCaseWriter::class.java)
                .to(RPCTestCaseWriter::class.java)
        bind(RPCTestCaseWriter::class.java)
            .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
            .asEagerSingleton()
    }
}