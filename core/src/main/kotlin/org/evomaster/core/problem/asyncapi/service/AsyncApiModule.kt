package org.evomaster.core.problem.asyncapi.service

import com.google.inject.TypeLiteral
import org.evomaster.core.output.service.NoTestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.problem.enterprise.service.EnterpriseSampler
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.remote.service.RemoteControllerImplementation
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.FlakinessDetector
import org.evomaster.core.search.service.Minimizer
import org.evomaster.core.search.service.Sampler
import org.evomaster.core.search.service.mutator.Mutator
import org.evomaster.core.search.service.mutator.StandardMutator
import org.evomaster.core.search.service.mutator.StructureMutator

/**
 * The services a search over an AsyncAPI service is made of.
 *
 * One module serves both white-box and black-box mode, and the driver is bound unconditionally:
 * it is what holds the connection to the broker, so it takes part either way (see
 * [org.evomaster.core.EMConfig.usesDriver]).
 *
 * No test cases are written yet, which [org.evomaster.core.EMConfig] enforces by requiring
 * `--createTests false`.
 */
class AsyncApiModule : EnterpriseModule() {

    override fun configure() {

        /*
            No super.configure(): what EnterpriseModule binds there is REST-only, and the RPC,
            GraphQL and Web modules leave it out the same way.
         */

        bind(object : TypeLiteral<EnterpriseSampler<AsyncApiIndividual>>() {})
            .to(AsyncApiSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<AsyncApiIndividual>>() {})
            .to(AsyncApiSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Sampler<*>>() {})
            .to(AsyncApiSampler::class.java)
            .asEagerSingleton()

        bind(AsyncApiSampler::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {})
            .to(AsyncApiBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<FitnessFunction<*>>() {})
            .to(AsyncApiBlackBoxFitness::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<AsyncApiIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>() {})
            .to(object : TypeLiteral<Minimizer<AsyncApiIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<AsyncApiIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<*>>() {})
            .to(object : TypeLiteral<FlakinessDetector<AsyncApiIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<AsyncApiIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
            .to(object : TypeLiteral<Archive<AsyncApiIndividual>>() {})

        bind(Archive::class.java)
            .to(object : TypeLiteral<Archive<AsyncApiIndividual>>() {})

        bind(RemoteController::class.java)
            .to(RemoteControllerImplementation::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Mutator<AsyncApiIndividual>>() {})
            .to(object : TypeLiteral<StandardMutator<AsyncApiIndividual>>() {})
            .asEagerSingleton()

        bind(StructureMutator::class.java)
            .to(AsyncApiStructureMutator::class.java)
            .asEagerSingleton()

        bind(TestCaseWriter::class.java)
            .to(NoTestCaseWriter::class.java)
            .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
            .asEagerSingleton()
    }
}
