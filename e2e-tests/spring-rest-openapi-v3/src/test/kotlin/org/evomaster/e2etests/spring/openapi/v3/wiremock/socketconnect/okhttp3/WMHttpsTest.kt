package org.evomaster.e2etests.spring.openapi.v3.wiremock.socketconnect.okhttp3

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.BufferedReader
import java.io.IOException
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*


class WMHttpsTest {

    //https://stackoverflow.com/questions/25509296/trusting-all-certificates-with-okhttp
    //https://blog.arkey.fr/2017/10/19/self-signed-certificates-in-java.en/
    //https://wiremock.org/docs/https/
    //https://github.com/square/okhttp/blob/master/okhttp-tls/README.md
    //https://sslcontext-kickstart.com/client/okhttp.html
    //https://www.baeldung.com/okhttp-client-trust-all-certificates
    //https://anil-gudigar.medium.com/ssl-connection-using-retrofit-and-okhttp-81a9efbc347

    companion object{

        val host = "127.0.0.12"
        val url = URL("https://$host:8443/api/string")

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOf()
                }
            }
        )

        private val wm = WireMockServer(
            wireMockConfig()
                .httpsPort(8443)
                .bindAddress("127.0.0.12")
                .extensions(ResponseTemplateTransformer(false))
        )

        @BeforeAll
        @JvmStatic
        fun setup(){
            wm.start()

            wm.stubFor(
                WireMock.any(WireMock.anyUrl())
                    .atPriority(100)
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(201)
                            .withBody("Hello")
                    )
            )
        }

        @AfterAll
        @JvmStatic
        fun terminate(){
            wm.stop()
        }
    }


    @Test
    fun testUrlConnectionHttps(){
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

        val connection = url.openConnection()
        connection.setRequestProperty("accept", "application/json")
        val urlOpenData = connection.getInputStream().bufferedReader().use(BufferedReader::readText)
        assertEquals("Hello", urlOpenData)

    }

    @Test
    fun testOkClientHttps(){
        val sc = SSLContext.getInstance("SSL")

        /*
            somehow, the okHttpClient only works with Standalone jar (see https://wiremock.org/docs/running-standalone/)
            ie,
            java -jar wiremock-jre8-standalone-2.34.0.jar --bind-address 127.0.0.12 --https-port 8443 --verbose

            curl -X POST --data '{ "request": { "url": "/api/string", "method": "GET" }, "response": { "status": 200, "body": "Hello" }}' http://127.0.0.2:8080/__admin/mappings/new

            Tried to employ own self-assigned certificate generated with KeyStore java, and
            install it into Java JDK CA Certificate key store path
            it shows the same problem with the default certificate of WM

            Tried to import the standalone jar, then run it with
                    `WireMockServerRunner.main(*args)`
            does not work

            In addition, tried with Junit5 + Wiremock (see https://wiremock.org/docs/junit-jupiter/)
            does not work with okHttpClient3

         */

        sc.init(null, trustAllCerts, SecureRandom())

        val okClientBuilder = OkHttpClient.Builder()
            .sslSocketFactory(sc.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
        val client = okClientBuilder.build()

        val request = Request.Builder().url(url).build()


        /*
            1st: java.io.IOException: cipherSuite == SSL_NULL_WITH_NULL_NULL

            at okhttp3.Handshake$Companion.get(Handshake.kt:151)
            at okhttp3.internal.connection.RealConnection.connectTls(RealConnection.kt:382)
            at okhttp3.internal.connection.RealConnection.establishProtocol(RealConnection.kt:337)

            2nd: javax.net.ssl.SSLException: readHandshakeRecord

            at sun.security.ssl.SSLSocketImpl.readHandshakeRecord(SSLSocketImpl.java:1210)
            at sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:401)
            at sun.security.ssl.SSLSocketImpl.startHandshake(SSLSocketImpl.java:373)
            at okhttp3.internal.connection.RealConnection.connectTls(RealConnection.kt:379)

         */
        assertThrows<Exception> {
            client.newCall(request).execute()
        }

        assertThrows<Exception> {
            /*
                it is SSLException on my windows, but SocketException on Andrea's windows and CI
             */
            client.newCall(request).execute()
        }

    }

}