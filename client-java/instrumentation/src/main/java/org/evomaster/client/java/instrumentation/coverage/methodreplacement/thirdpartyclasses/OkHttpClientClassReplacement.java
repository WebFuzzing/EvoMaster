package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.shared.PreDefinedSSLInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils.collectExternalServiceInfo;

public class OkHttpClientClassReplacement extends ThirdPartyMethodReplacementClass {

    private static ThreadLocal<OkHttpClient> instance = new ThreadLocal<>();

    private static final OkHttpClientClassReplacement singleton = new OkHttpClientClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "com.squareup.okhttp.OkHttpClient";
    }


    public static OkHttpClient consumeInstance(){

        OkHttpClient client = instance.get();
        if(client == null){
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return client;
    }

    private static void addInstance(OkHttpClient x){
        OkHttpClient client = instance.get();
        if(client != null){
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient_constructor",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            replacingConstructor = true
    )
    public static void OkHttpClient()  {

        Constructor original = getOriginalConstructor(singleton, "okhttpclient_constructor");

        try {

            OkHttpClient client = (OkHttpClient) original.newInstance();
            client.setSslSocketFactory(PreDefinedSSLInfo.getTrustAllSSLSocketFactory());
            client.setHostnameVerifier(PreDefinedSSLInfo.allowAllHostNames());
            addInstance(client);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        }

    }


    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient_setSslSocketFactory",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET
    )
    public static OkHttpClient setSslSocketFactory(Object caller, SSLSocketFactory sslSocketFactory) {
        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient_setSslSocketFactory", caller);

        try{
            return (OkHttpClient) original.invoke(caller, PreDefinedSSLInfo.getTrustAllSSLSocketFactory());
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient_setHostnameVerifier",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET
    )
    public static OkHttpClient setHostnameVerifier(Object caller, HostnameVerifier hostnameVerifier) {
        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient_setHostnameVerifier", caller);

        try{
            return (OkHttpClient) original.invoke(caller, PreDefinedSSLInfo.allowAllHostNames());
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient_newCall",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET
    )
    public static Call newCall(Object caller, Request request){
        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient_newCall", caller);


        Request replaced = request;
        HttpUrl url = request.httpUrl();
        if (url.scheme().equalsIgnoreCase("https") || url.scheme().equalsIgnoreCase("http")){
            ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(url.scheme(), url.host(), url.port());
            String[] ipAndPort = collectExternalServiceInfo(remoteHostInfo, url.port());
            String replacedURL = url.scheme()+"://"+ipAndPort[0]+":"+ipAndPort[1]+url.encodedPath();
            if (url.encodedQuery() != null && !url.encodedQuery().isEmpty())
                replacedURL = replacedURL + "?" + url.encodedQuery();
            replaced = new Request.Builder().url(replacedURL).build();
        }
        try{
            return (Call) original.invoke(caller, replaced);
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }


}
