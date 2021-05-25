package org.evomaster.core.problem.graphql.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.GraphQLTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

class GraphQLModule : AbstractModule() {

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
                .to(GraphQLFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<GraphQLIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<GraphQLIndividual>>() {})

        bind(RemoteController::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<GraphQLIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<GraphQLIndividual>>() {})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(GraphQLStructureMutator::class.java)
                .asEagerSingleton()

        bind(TestCaseWriter::class.java)
                .to(GraphQLTestCaseWriter::class.java)
                .asEagerSingleton()
    }
}