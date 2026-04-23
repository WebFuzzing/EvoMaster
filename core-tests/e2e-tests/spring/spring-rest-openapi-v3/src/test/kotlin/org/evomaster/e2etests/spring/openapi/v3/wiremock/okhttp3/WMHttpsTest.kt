package org.evomaster.e2etests.spring.openapi.v3.wiremock.okhttp3

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*


class WMHttpsTest {


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
    fun testOkClient3Https(){
        val sc = SSLContext.getInstance("SSL")

        sc.init(null, trustAllCerts, SecureRandom())

        val okClientBuilder = okhttp3.OkHttpClient.Builder()
            .sslSocketFactory(sc.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
        val client = okClientBuilder.build()

        val request = okhttp3.Request.Builder().url(url).build()

        val data = client.newCall(request).execute()
        val res = data.body?.string()
        data.close()
        assertEquals("Hello", res)

    }

    @Test
    fun testOkClientHttps(){
        val sc = SSLContext.getInstance("SSL")

        sc.init(null, trustAllCerts, SecureRandom())

        val client = com.squareup.okhttp.OkHttpClient()
        client.setSslSocketFactory(sc.socketFactory)
        client.setHostnameVerifier({ _, _ -> true })

        val request = com.squareup.okhttp.Request.Builder().url(url).build()

        val data = client.newCall(request).execute()
        val res = data.body()?.string()
        assertEquals("Hello", res)

    }

}