package org.evomaster.client.java.instrumentation.shared;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * in order to support communications with WM using https
 * this class contains info for configuring clients
 */
public class PreDefinedSSLInfo {

    private static final PreDefinedSSLInfo singleton = new PreDefinedSSLInfo();

    private final SSLContext sslContext;

    public PreDefinedSSLInfo(){
        sslContext = sslContext();
    }

    /**
     * @return pre-defined HostnameVerifier which accepts all host names
     */
    public static HostnameVerifier allowAllHostNames() {
        return (hostname, sslSession) -> true;
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {
        public static final X509TrustManager singleton = new TrustAllX509TrustManager();

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException { }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException { }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private SSLContext sslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{TrustAllX509TrustManager.singleton}, null);
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return pre-defined SSLContext which ignores ssl certificate checking
     */
    public static SSLContext getSSLContext(){
        return singleton.sslContext;
    }

    /**
     * @return pre-defined SSLSocketFactory which ignores ssl certificate checking
     */
    public static SSLSocketFactory getTrustAllSSLSocketFactory(){
        return singleton.sslContext.getSocketFactory();
    }

    /**
     * @return pre-defined X509TrustManager which ignores ssl certificate checking
     */
    public static X509TrustManager getTrustAllX509TrustManager(){
        return TrustAllX509TrustManager.singleton;
    }

    /**
     * configure HttpsURLConnection in order to accept all certificate
     */
    public static void setTrustAllForHttpsURLConnection(){
        HttpsURLConnection.setDefaultSSLSocketFactory(getTrustAllSSLSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(allowAllHostNames());
    }
}
