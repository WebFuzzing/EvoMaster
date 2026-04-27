package org.evomaster.core.problem.asyncapi.service.module

import com.google.inject.Provides
import com.google.inject.Singleton
import com.google.inject.TypeLiteral
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.AsyncAPITestCaseWriter
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.output.service.TestSuiteWriter
import org.evomaster.core.problem.asyncapi.broker.KafkaBrokerClient
import org.evomaster.core.problem.asyncapi.broker.MessageBrokerClient
import org.evomaster.core.problem.asyncapi.data.AsyncAPIIndividual
import org.evomaster.core.problem.enterprise.service.EnterpriseModule
import org.evomaster.core.search.service.Archive
import org.evomaster.core.search.service.FlakinessDetector
import org.evomaster.core.search.service.Minimizer

/**
 * Guice bindings shared by both AsyncAPI white-box and black-box modes.
 *
 * The white-box module is not implemented in the starter slice; this base
 * exists so M5 can drop in `AsyncAPIModule` without re-doing wiring.
 */
open class AsyncAPIBaseModule : EnterpriseModule() {

    override fun configure() {
        super.configure()

        bind(TestCaseWriter::class.java)
            .to(AsyncAPITestCaseWriter::class.java)
            .asEagerSingleton()

        bind(TestSuiteWriter::class.java)
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Minimizer<*>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<FlakinessDetector<*>>() {})
            .to(object : TypeLiteral<FlakinessDetector<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(object : TypeLiteral<Archive<*>>() {})
            .to(object : TypeLiteral<Archive<AsyncAPIIndividual>>() {})
            .asEagerSingleton()

        bind(Archive::class.java)
            .to(object : TypeLiteral<Archive<AsyncAPIIndividual>>() {})
            .asEagerSingleton()
    }

    /**
     * Construct the broker bridge from the runtime config so the same
     * instance is reused across the search.  Producing it here keeps the
     * fitness function free of broker-construction logic and lets later
     * additions (MQTT, etc.) plug in by replacing this provider.
     */
    @Provides
    @Singleton
    fun provideBrokerClient(config: EMConfig): MessageBrokerClient {
        if (config.bbBrokerUrl.isBlank()) {
            throw IllegalStateException(
                "AsyncAPI requires --bbBrokerUrl to be set (e.g. localhost:9092)"
            )
        }
        return KafkaBrokerClient(bootstrapServers = config.bbBrokerUrl)
    }
}
