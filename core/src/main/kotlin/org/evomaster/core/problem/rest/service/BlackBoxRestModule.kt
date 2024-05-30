package org.evomaster.core.problem.rest.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.seeding.service.rest.PirToRest

class BlackBoxRestModule(
        val usingRemoteController: Boolean
): RestBaseModule(){

    override fun configure() {

        super.configure()

        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(AbstractRestSampler::class.java)
                .to(RestSampler::class.java)
                .asEagerSingleton()

        bind(RestSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
            .to(BlackBoxRestFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(BlackBoxRestFitness::class.java)
            .asEagerSingleton()



        if(usingRemoteController) {
            bind(RemoteController::class.java)
                    .to(RemoteControllerImplementation::class.java)
                    .asEagerSingleton()
        }

    }
}