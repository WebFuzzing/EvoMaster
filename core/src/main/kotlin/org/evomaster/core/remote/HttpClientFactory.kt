package org.evomaster.core.remote

import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder

object HttpClientFactory {


    /**
     * Create a Jersey Client to make HTTP calls.
     * All certificates are trusted by default.
     * The idea here is that, even if SSL certificates are expired, we should still be able to fuzz a web application
     */
    fun createTrustingJerseyClient(followRedirects : Boolean = false, readTimeout: Int = 30_000) : Client{

        /*
         * This code is taken and adapted from
         * https://stackoverflow.com/questions/19540289/how-to-fix-the-java-security-cert-certificateexception-no-subject-alternative
         */

        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                //return null;
                //based on:
                //https://stackoverflow.com/questions/6047996/ignore-self-signed-ssl-cert-using-jersey-client
                return arrayOfNulls(0)
            }
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            }
        )


        // Install the all-trusting trust manager
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, SecureRandom())
        //HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

        // Create all-trusting host name verifier
        val allHostsValid = HostnameVerifier { _, _ -> true }

        // Install the all-trusting host verifier
        //HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
        //SSLContext.setDefault(sc)

        return ClientBuilder.newBuilder()
            .sslContext(sc)
            .hostnameVerifier(allHostsValid)
            .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
            .property(ClientProperties.READ_TIMEOUT, readTimeout)
            //workaround bug in Jersey client
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
            .property(ClientProperties.FOLLOW_REDIRECTS, followRedirects)
            .build()
    }
}