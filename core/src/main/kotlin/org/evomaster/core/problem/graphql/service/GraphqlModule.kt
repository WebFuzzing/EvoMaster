package org.evomaster.core.problem.graphql.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.graphql.GraphqlIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StructureMutator

class GraphqlModule : AbstractModule(){
    override fun configure() {
        bind(object : TypeLiteral<Sampler<GraphqlIndividual>>() {})
                .to(GraphqlSampler::class.java)
                .asEagerSingleton()

        bind(GraphqlSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<GraphqlIndividual>>() {})
                .to(GraphqlFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<GraphqlIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<GraphqlIndividual>>() {})

        bind(RemoteController::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<GraphqlIndividual>>() {})
                .to(object : TypeLiteral<StandardMutator<GraphqlIndividual>>(){})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(GraphqlStructureMutator::class.java)
                .asEagerSingleton()
    }
}