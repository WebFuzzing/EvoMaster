package org.evomaster.core.problem.graphql.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.GraphQLTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler

class GraphQLBlackBoxModule(
        val usingRemoteController: Boolean
): AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<GraphQLIndividual>>() {})
                .to(GraphQLSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(GraphQLSampler::class.java)
            .asEagerSingleton()

        bind(GraphQLSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<GraphQLIndividual>>() {})
                .to(GraphQLBlackBoxFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(GraphQLBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<GraphQLIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<GraphQLIndividual>>() {})

        bind(object : TypeLiteral<Minimizer<GraphQLIndividual>>(){})
                .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>(){})
                .asEagerSingleton()


        if(usingRemoteController) {
            bind(RemoteController::class.java)
                .to(RemoteControllerImplementation::class.java)
                .asEagerSingleton()
        }

        bind(TestCaseWriter::class.java)
                .to(GraphQLTestCaseWriter::class.java)
                .asEagerSingleton()
    }
}