package org.evomaster.core.problem.graphql.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.AlgorithmRegistry
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.GraphQLTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.FlakinessDetector
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler

class GraphQLBlackBoxModule(
        val usingRemoteController: Boolean
): EnterpriseModule(){

    override fun configure() {
        super.configure()
        binder().requireExplicitBindings()

        if(usingRemoteController) {
            bindRemoteController()
        }

        AlgorithmRegistry.keys(EMConfig.ProblemType.GRAPHQL)
            .forEach { bind(it) }

        bind(object : TypeLiteral<EnterpriseSampler<GraphQLIndividual>>() {})
            .to(GraphQLSampler::class.java)
        bind(object : TypeLiteral<Sampler<GraphQLIndividual>>() {})
                .to(GraphQLSampler::class.java)
        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(GraphQLSampler::class.java)
        bind(GraphQLSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<GraphQLIndividual>>() {})
                .to(GraphQLBlackBoxFitness::class.java)
        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(GraphQLBlackBoxFitness::class.java)
        bind(GraphQLFitness::class.java)
            .to(GraphQLBlackBoxFitness::class.java)
        bind(GraphQLBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<GraphQLIndividual>>() {})
                .asEagerSingleton()
        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<GraphQLIndividual>>() {})
        bind(Archive::class.java)
            .to(object : TypeLiteral<Archive<GraphQLIndividual>>() {})


        bind(object : TypeLiteral<Minimizer<GraphQLIndividual>>(){})
                .asEagerSingleton()
        bind(object : TypeLiteral<Minimizer<*>>(){})
            .to(object : TypeLiteral<Minimizer<GraphQLIndividual>>() {})

        bind(object : TypeLiteral<FlakinessDetector<GraphQLIndividual>>(){})
            .asEagerSingleton()
        bind(object : TypeLiteral<FlakinessDetector<*>>(){})
            .to(object : TypeLiteral<FlakinessDetector<GraphQLIndividual>>(){})

        bind(TestCaseWriter::class.java)
                .to(GraphQLTestCaseWriter::class.java)
        bind(GraphQLTestCaseWriter::class.java)
            .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
            .asEagerSingleton()
    }
}