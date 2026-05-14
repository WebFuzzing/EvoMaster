package org.evomaster.core.problem.asyncapi.service.module

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

        // Bind LazyBrokerClient so Guice manages its (optional) RemoteController
        // injection.  The interface alias lets fitness/sampler depend on the
        // protocol-agnostic type.
        bind(LazyBrokerClient::class.java)
            .asEagerSingleton()
        bind(MessageBrokerClient::class.java)
            .to(LazyBrokerClient::class.java)
            .asEagerSingleton()
    }
}

/**
 * Wraps a [KafkaBrokerClient] but defers bootstrap-URL resolution until
 * the first call to [connect].  Lets us bind the broker as an eager
 * Guice singleton without forcing the URL to be known at injector-creation
 * time — important for white-box, where the URL comes from the EM Driver
 * after `startSUT` has fired.
 *
 * Black-box runs read [EMConfig.bbBrokerUrl] directly; white-box runs ask
 * the driver via the optionally-injected [RemoteController].
 */
class LazyBrokerClient @com.google.inject.Inject constructor(
    private val config: EMConfig
) : MessageBrokerClient {

    @com.google.inject.Inject(optional = true)
    private var rc: org.evomaster.core.remote.service.RemoteController? = null

    @Volatile
    private var delegate: KafkaBrokerClient? = null

    private fun resolveBootstrapServers(): String {
        if (config.bbBrokerUrl.isNotBlank()) {
            return config.bbBrokerUrl
        }
        val controller = rc
            ?: throw IllegalStateException("AsyncAPI requires either --bbBrokerUrl (black-box) or an EM Driver (white-box) reporting an AsyncAPIProblem")
        val info = controller.getSutInfo()
            ?: throw IllegalStateException("AsyncAPI white-box: EM Driver returned no SUT info")
        val bootstrap = info.asyncAPIProblem?.brokerBootstrapServers
            ?: throw IllegalStateException("AsyncAPI white-box: EM Driver's AsyncAPIProblem did not report brokerBootstrapServers")
        return bootstrap
    }

    @Synchronized
    private fun ensure(): KafkaBrokerClient {
        delegate?.let { return it }
        val client = KafkaBrokerClient(
            bootstrapServers = resolveBootstrapServers(),
            auth = resolveAuth()
        )
        delegate = client
        return client
    }

    private fun resolveAuth(): org.evomaster.core.problem.asyncapi.broker.AsyncAPIBrokerAuthInfo {
        // White-box driver-supplied broker auth is a follow-up; black-box reads
        // CLI flags now. Returning NoAuth when the flag is unset keeps existing
        // unauthenticated test brokers (Testcontainers, local KRaft) working
        // without per-test plumbing.
        return when (config.bbBrokerAuthType) {
            EMConfig.BrokerAuthType.NONE ->
                org.evomaster.core.problem.asyncapi.broker.AsyncAPIBrokerAuthInfo.NoAuth
            EMConfig.BrokerAuthType.SASL_PLAIN ->
                org.evomaster.core.problem.asyncapi.broker.AsyncAPIBrokerAuthInfo.SaslPlain(
                    username = config.bbBrokerUsername,
                    password = config.bbBrokerPassword,
                    tls = config.bbBrokerSaslOverTls
                )
            EMConfig.BrokerAuthType.SASL_SCRAM_256 ->
                org.evomaster.core.problem.asyncapi.broker.AsyncAPIBrokerAuthInfo.SaslScramSha256(
                    username = config.bbBrokerUsername,
                    password = config.bbBrokerPassword,
                    tls = config.bbBrokerSaslOverTls
                )
            EMConfig.BrokerAuthType.SSL ->
                org.evomaster.core.problem.asyncapi.broker.AsyncAPIBrokerAuthInfo.Ssl(
                    truststorePath = config.bbBrokerTruststorePath.ifBlank { null },
                    truststorePassword = config.bbBrokerTruststorePassword.ifBlank { null },
                    keystorePath = config.bbBrokerKeystorePath.ifBlank { null },
                    keystorePassword = config.bbBrokerKeystorePassword.ifBlank { null }
                )
        }
    }

    override fun connect() = ensure().connect()

    override fun publish(
        channel: String,
        key: String?,
        headers: Map<String, ByteArray>,
        payload: ByteArray
    ) = ensure().publish(channel, key, headers, payload)

    override fun awaitFirstMatching(
        channel: String,
        predicate: (Map<String, ByteArray>) -> Boolean,
        timeoutMs: Long
    ) = ensure().awaitFirstMatching(channel, predicate, timeoutMs)

    override fun close() {
        delegate?.close()
        delegate = null
    }
}
