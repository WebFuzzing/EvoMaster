package org.evomaster.core.problem.graphql.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.search.service.Sampler

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


        //TODO
    }
}