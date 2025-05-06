package org.evomaster.core.problem.webfrontend.service

import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.output.service.WebTestCaseWriter
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.problem.webfrontend.WebIndividual
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * Class defining all the beans needed to enable EM to
 * handle Web Applications
 *
 * TODO See equivalent RestModule
 */
class WebModule: EnterpriseModule() {

    override fun configure() {
        bind(object : TypeLiteral<Sampler<WebIndividual>>() {})
            .to(WebSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(WebSampler::class.java)
            .asEagerSingleton()

        bind(WebSampler::class.java)
            .asEagerSingleton()


        bind(object : TypeLiteral<Minimizer<WebIndividual>>(){})
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>(){})
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<WebIndividual>>() {})
            .to(WebFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(WebFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<WebIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
            .to(object : TypeLiteral<Archive<WebIndividual>>() {})

        bind(object : TypeLiteral<Minimizer<WebIndividual>>(){})
                .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>(){})
                .asEagerSingleton()


        bind(RemoteController::class.java)
            .to(RemoteControllerImplementation::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<WebIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<WebIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(WebStructureMutator::class.java)
            .asEagerSingleton()

        bind(TestCaseWriter::class.java)
            .to(WebTestCaseWriter::class.java)
            .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
            .asEagerSingleton()

        bind(BrowserController::class.java)
            .asEagerSingleton()

        bind(WebPageIdentifier::class.java)
            .asEagerSingleton()

        bind(WebGlobalState::class.java)
            .asEagerSingleton()
    }
}