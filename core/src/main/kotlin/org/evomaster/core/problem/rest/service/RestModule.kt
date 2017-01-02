package org.evomaster.core.problem.rest.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.search.mutator.RandomMutator
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Mutator
import org.evomaster.core.search.service.Sampler


class RestModule : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(RestSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
                .to(RestFitness::class.java)
                .asEagerSingleton()

        //TODO this will need updating
        bind(object : TypeLiteral<Mutator<RestIndividual>>() {})
                .to(object : TypeLiteral<RandomMutator<RestIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<RestIndividual>>() {})
                .asEagerSingleton()

    }
}