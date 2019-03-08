package org.evomaster.core.problem.rest.serviceII

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.service.RestFitness
import org.evomaster.core.problem.rest.service.RestSampler
import org.evomaster.core.problem.rest2.RestResourceMutator
import org.evomaster.core.problem.rest2.RestResourceStructureMutator
import org.evomaster.core.problem.rest2.resources.ResourceManageService
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.*
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StructureMutator
import javax.annotation.Resource


class RestModuleII : AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<RestIndividualII>>() {})
                .to(RestSamplerII::class.java)
                .asEagerSingleton()

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
                .to(RestResourceMutator::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StandardMutator<RestIndividualII>>() {})
                .to(RestResourceMutator::class.java)
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(RestResourceStructureMutator::class.java)
                .asEagerSingleton()

        bind(ResourceManageService::class.java)
                .asEagerSingleton()

    }
}