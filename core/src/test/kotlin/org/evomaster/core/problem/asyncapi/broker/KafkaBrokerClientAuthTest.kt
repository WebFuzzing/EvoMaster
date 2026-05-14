package org.evomaster.core.problem.asyncapi.broker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit-level coverage of the SASL / SSL property-mapping introduced in M9-PR3.
 * Keeps the assertions on the pure function [KafkaBrokerClient.authProps] so
 * the mapping is verified without spinning up a Kafka container (the
 * Testcontainers-backed roundtrip belongs in KafkaBrokerClientTest, which
 * extends naturally once the CI host can pull an image that supports SASL).
 */
class KafkaBrokerClientAuthTest {

    @Test
    fun noAuthEmitsEmptyProps() {
        val props = KafkaBrokerClient.authProps(AsyncAPIBrokerAuthInfo.NoAuth)
        assertTrue(props.isEmpty, "NoAuth should set no security props")
    }

    @Test
    fun saslPlainOverPlaintextSetsExpectedKeys() {
        val props = KafkaBrokerClient.authProps(
            AsyncAPIBrokerAuthInfo.SaslPlain("alice", "s3cret", tls = false)
        )
        assertEquals("SASL_PLAINTEXT", props["security.protocol"])
        assertEquals("PLAIN", props["sasl.mechanism"])
        val jaas = props["sasl.jaas.config"] as String
        assertTrue(jaas.startsWith("org.apache.kafka.common.security.plain.PlainLoginModule required"), jaas)
        assertTrue(jaas.contains("username=\"alice\""), jaas)
        assertTrue(jaas.contains("password=\"s3cret\""), jaas)
        assertTrue(jaas.endsWith(";"), jaas)
    }

    @Test
    fun saslPlainOverTlsSwitchesProtocol() {
        val props = KafkaBrokerClient.authProps(
            AsyncAPIBrokerAuthInfo.SaslPlain("bob", "pw", tls = true)
        )
        assertEquals("SASL_SSL", props["security.protocol"])
        assertEquals("PLAIN", props["sasl.mechanism"])
    }

    @Test
    fun saslScram256UsesScramLoginModuleAndCorrectMechanism() {
        val props = KafkaBrokerClient.authProps(
            AsyncAPIBrokerAuthInfo.SaslScramSha256("svc", "scr@m", tls = false)
        )
        assertEquals("SASL_PLAINTEXT", props["security.protocol"])
        assertEquals("SCRAM-SHA-256", props["sasl.mechanism"])
        val jaas = props["sasl.jaas.config"] as String
        assertTrue(
            jaas.startsWith("org.apache.kafka.common.security.scram.ScramLoginModule required"),
            jaas
        )
        assertTrue(jaas.contains("username=\"svc\""), jaas)
        assertTrue(jaas.contains("password=\"scr@m\""), jaas)
    }

    @Test
    fun jaasEscapesQuotesAndBackslashesInPassword() {
        // Real Kafka credentials in the wild often contain " and \; the
        // mapper must escape them so the JAAS line stays parsable.
        val props = KafkaBrokerClient.authProps(
            AsyncAPIBrokerAuthInfo.SaslPlain("u", """a"b\c""", tls = false)
        )
        val jaas = props["sasl.jaas.config"] as String
        // a" → a\", b\ → b\\, so we expect: password="a\"b\\c"
        assertTrue(jaas.contains("""password="a\"b\\c""""), "jaas was: $jaas")
    }

    @Test
    fun sslSetsProtocolAndKeystoreProps() {
        val props = KafkaBrokerClient.authProps(
            AsyncAPIBrokerAuthInfo.Ssl(
                truststorePath = "/tls/truststore.jks",
                truststorePassword = "ts-pw",
                keystorePath = "/tls/keystore.jks",
                keystorePassword = "ks-pw"
            )
        )
        assertEquals("SSL", props["security.protocol"])
        assertEquals("/tls/truststore.jks", props["ssl.truststore.location"])
        assertEquals("ts-pw", props["ssl.truststore.password"])
        assertEquals("/tls/keystore.jks", props["ssl.keystore.location"])
        assertEquals("ks-pw", props["ssl.keystore.password"])
    }

    @Test
    fun sslWithServerOnlyHandshakeOmitsKeystoreProps() {
        // Common case: mTLS not in use; only the truststore is set so the
        // client can verify the server cert.
        val props = KafkaBrokerClient.authProps(
            AsyncAPIBrokerAuthInfo.Ssl(
                truststorePath = "/tls/truststore.jks",
                truststorePassword = "ts-pw"
            )
        )
        assertEquals("SSL", props["security.protocol"])
        assertEquals("/tls/truststore.jks", props["ssl.truststore.location"])
        assertNull(props["ssl.keystore.location"])
        assertNull(props["ssl.keystore.password"])
    }
}
