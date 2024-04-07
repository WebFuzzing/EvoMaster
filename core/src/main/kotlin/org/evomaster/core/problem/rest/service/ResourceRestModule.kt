package org.evomaster.core.problem.rest.service

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.RestTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.externalservice.httpws.service.HarvestActualHttpWsResponseHandler
import org.evomaster.core.problem.externalservice.httpws.service.HttpWsExternalServiceHandler
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator
import org.evomaster.core.seeding.service.rest.PirToRest


class ResourceRestModule(private val bindRemote : Boolean = true) : AbstractModule(){

    override fun configure() {

        if(bindRemote){
            /*
                Governator does not seem to have a way to override bindings for testing :(
                so we do it manually
             */
            bind(RemoteController::class.java)
                    .to(RemoteControllerImplementation::class.java)
                    .asEagerSingleton()
        }

        bind(object : TypeLiteral<Sampler<RestIndividual>>() {})
                .to(ResourceSampler::class.java)
                .asEagerSingleton()
        bind(object : TypeLiteral<AbstractRestSampler>() {})
                .to(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
                .to(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(AbstractRestSampler::class.java)
                .to(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(ResourceSampler::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<RestIndividual>>() {})
                .to(RestResourceFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
                .to(RestResourceFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<AbstractRestFitness>() {})
                .to(RestResourceFitness::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<RestIndividual>>(){})
                .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>(){})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<RestIndividual>>() {})
                .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
                .to(object : TypeLiteral<Archive<RestIndividual>>() {})

        bind(object : TypeLiteral<Mutator<RestIndividual>>() {})
                .to(ResourceRestMutator::class.java)
                .asEagerSingleton()

        bind(object : TypeLiteral<StandardMutator<RestIndividual>>() {})
                .to(ResourceRestMutator::class.java)
                .asEagerSingleton()

        bind(ResourceRestMutator::class.java)
                .asEagerSingleton()

        bind(StructureMutator::class.java)
                .to(RestResourceStructureMutator::class.java)
                .asEagerSingleton()

        bind(ResourceManageService::class.java)
                .asEagerSingleton()

        bind(ResourceDepManageService::class.java)
                .asEagerSingleton()

        bind(TestCaseWriter::class.java)
                .to(RestTestCaseWriter::class.java)
                .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
                .asEagerSingleton()

        bind(HttpWsExternalServiceHandler::class.java)
                .asEagerSingleton()

        bind(HarvestActualHttpWsResponseHandler::class.java)
            .asEagerSingleton()

        bind(SecurityRest::class.java)
            .asEagerSingleton()

        bind(PirToRest::class.java)
            .asEagerSingleton()

    }
}