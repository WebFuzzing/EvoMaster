package org.evomaster.client.java.instrumentation;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class PreDefinedSSLInfo {

    private static final PreDefinedSSLInfo singleton = new PreDefinedSSLInfo();

    private final SSLContext sslContext;

    public PreDefinedSSLInfo(){
        sslContext = sslContext();
    }

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

    public static SSLContext getSSLContext(){
        return singleton.sslContext;
    }

    public static SSLSocketFactory getTrustAllSSLSocketFactory(){
        return singleton.sslContext.getSocketFactory();
    }

    public static X509TrustManager getTrustAllX509TrustManager(){
        return TrustAllX509TrustManager.singleton;
    }

}
