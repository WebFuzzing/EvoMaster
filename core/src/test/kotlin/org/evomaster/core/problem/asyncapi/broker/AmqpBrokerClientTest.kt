package org.evomaster.core.problem.asyncapi.broker

import org.evomaster.core.EMConfig
import org.evomaster.core.config.ConfigProblemException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * M11-PR9: confirms the URI-resolution contract for the AMQP transport
 * and the EMConfig validation rules that gate ill-formed AMQP runs.
 *
 * The richer broker behaviour (publish + consume against a live
 * RabbitMQ) is exercised by the validation-corpus runs against the
 * bookworm-family SUTs, not by an in-tree integration test (would need
 * a RabbitMQ Testcontainer or an embedded broker, neither of which is
 * worth adding for the starter slice).
 */
class AmqpBrokerClientTest {

    @Test
    fun fullAmqpUriPassesThroughUntouched() {
        val uri = AmqpBrokerClient.resolveUri("amqp://guest:guest@localhost:5672/")
        assertEquals("amqp://guest:guest@localhost:5672/", uri.toString())
    }

    @Test
    fun fullAmqpsUriPassesThroughUntouched() {
        val uri = AmqpBrokerClient.resolveUri("amqps://user:secret@rabbit.example.com:5671/prod")
        assertEquals("amqps://user:secret@rabbit.example.com:5671/prod", uri.toString())
    }

    @Test
    fun bareHostPortGetsAmqpPrefix() {
        val uri = AmqpBrokerClient.resolveUri("localhost:5672")
        assertEquals("amqp://localhost:5672", uri.toString())
    }

    @Test
    fun bareHostnameGetsAmqpPrefix() {
        val uri = AmqpBrokerClient.resolveUri("rabbit")
        assertEquals("amqp://rabbit", uri.toString())
    }

    @Test
    fun whitespaceIsTrimmedBeforeResolution() {
        val uri = AmqpBrokerClient.resolveUri("  localhost:5672  ")
        assertEquals("amqp://localhost:5672", uri.toString())
    }

    @Test
    fun amqpTransportRejectsBrokerAuth() {
        val config = EMConfig()
        config.blackBox = true
        config.problemType = EMConfig.ProblemType.ASYNCAPI
        config.bbAsyncApiUrl = "file:///tmp/spec.yaml"
        config.bbBrokerUrl = "amqp://localhost:5672"
        config.bbBrokerTransport = EMConfig.BrokerTransport.AMQP
        config.bbBrokerAuthType = EMConfig.BrokerAuthType.SASL_PLAIN
        config.bbBrokerUsername = "u"
        config.bbBrokerPassword = "p"
        val ex = assertThrows(ConfigProblemException::class.java) { config.checkMultiFieldConstraints() }
        assert(ex.message!!.contains("AMQP transport reads credentials from --bbBrokerUrl")) {
            "expected error to point at the credentials-in-URI convention; got: ${ex.message}"
        }
    }

    @Test
    fun amqpTransportRejectsEmbedBroker() {
        val config = EMConfig()
        config.blackBox = true
        config.problemType = EMConfig.ProblemType.ASYNCAPI
        config.bbAsyncApiUrl = "file:///tmp/spec.yaml"
        config.bbBrokerUrl = "amqp://localhost:5672"
        config.bbBrokerTransport = EMConfig.BrokerTransport.AMQP
        config.asyncApiEmbedBroker = true
        val ex = assertThrows(ConfigProblemException::class.java) { config.checkMultiFieldConstraints() }
        assert(ex.message!!.contains("incompatible with --bbBrokerTransport=AMQP")) {
            "expected error to mention the embed-broker / AMQP incompatibility; got: ${ex.message}"
        }
    }
}
