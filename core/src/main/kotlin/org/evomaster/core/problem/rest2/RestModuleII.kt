package org.evomaster.core.problem.rest.serviceII

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.problem.rest.service.RestStructureMutator
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.mutator.StandardMutator
import org.evomaster.core.search.service.*


class RestModuleII : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<RestIndividualII>>() {})
                .to(RestSamplerII::class.java)
                .asEagerSingleton()

        //UpdatedByMan
        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(RestSamplerII::class.java)
                .asEagerSingleton()

        bind(RestSamplerII::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividualII>>() {})
                .to(RestFitnessII::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<RestFitness<RestIndividualII>>() {})
                .to(RestFitnessII::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<RestIndividualII>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<RestIndividualII>>() {})

        bind(RemoteController::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<RestIndividualII>>() {})
                .to(object : TypeLiteral<StandardMutator<RestIndividualII>>(){})
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(RestStructureMutator::class.java)
                .asEagerSingleton()

    }
}