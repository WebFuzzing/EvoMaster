package org.evomaster.core.problem.rest.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Sampler

class BlackBoxRestModule(
        val usingRemoteController: Boolean
): AbstractModule(){

    override fun configure() {
        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(RestSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
                .to(BlackBoxRestFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<RestIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<RestIndividual>>() {})

        if(usingRemoteController) {
            bind(RemoteController::class.java)
                    .asEagerSingleton()
        }
    }
}