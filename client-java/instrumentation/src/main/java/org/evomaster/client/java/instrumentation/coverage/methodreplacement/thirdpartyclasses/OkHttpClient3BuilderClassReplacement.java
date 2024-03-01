package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.shared.PreDefinedSSLInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyCast;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OkHttpClient3BuilderClassReplacement extends ThirdPartyMethodReplacementClass {

    private static ThreadLocal<Object> instance = new ThreadLocal<>();

    private static final OkHttpClient3BuilderClassReplacement singleton = new OkHttpClient3BuilderClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "okhttp3.OkHttpClient$Builder";
    }

    public static Boolean hasInstance(){
        return  instance.get() != null;
    }

    public static Object consumeInstance(){

        Object builder =  instance.get();
        if(builder == null){
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return builder;
    }

    private static void addInstance(Object x){
        Object builder =  instance.get();
        if(builder != null){
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient3_builder_constructor",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            replacingConstructor = true,
            castTo = "okhttp3.OkHttpClient$Builder"
    )
    public static void Builder()  throws Exception{

        Constructor original = getOriginalConstructor(singleton, "okhttpclient3_builder_constructor");

        try {
            Object builder =  original.newInstance();
            builder.getClass().getMethod("sslSocketFactory", SSLSocketFactory.class, X509TrustManager.class)
                    .invoke(builder, PreDefinedSSLInfo.getTrustAllSSLSocketFactory(), PreDefinedSSLInfo.getTrustAllX509TrustManager());
            builder.getClass().getMethod("hostnameVerifier", HostnameVerifier.class)
                    .invoke(builder,PreDefinedSSLInfo.allowAllHostNames());
//            builder.sslSocketFactory(PreDefinedSSLInfo.getTrustAllSSLSocketFactory(), PreDefinedSSLInfo.getTrustAllX509TrustManager());
//            builder.hostnameVerifier(PreDefinedSSLInfo.allowAllHostNames());
            addInstance(builder);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }

    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient3_builder_hostnameVerifier",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            castTo = "okhttp3.OkHttpClient$Builder"
    )
    public static Object hostnameVerifier (
            Object caller,
            @ThirdPartyCast(actualType = "javax.net.ssl.HostnameVerifier") Object hostnameVerifier)
    throws Exception {

        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient3_builder_hostnameVerifier", caller);

        try {
            return original.invoke(caller, PreDefinedSSLInfo.allowAllHostNames());
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (Exception) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient3_builder_sslSocketFactory",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            castTo = "okhttp3.OkHttpClient$Builder"
    )
    public static Object sslSocketFactory(
            Object caller,
            @ThirdPartyCast(actualType = "javax.net.ssl.SSLSocketFactory") Object sslSocketFactory,
            @ThirdPartyCast(actualType = "javax.net.ssl.X509TrustManager") Object trustManager)  {

        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient3_builder_sslSocketFactory", caller);

        try {
            return original.invoke(
                    caller, PreDefinedSSLInfo.getTrustAllSSLSocketFactory(), PreDefinedSSLInfo.getTrustAllX509TrustManager());
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient3_builder_sslSocketFactory_onearg",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            castTo = "okhttp3.OkHttpClient$Builder"
    )
    public static Object sslSocketFactory(
            Object caller,
            @ThirdPartyCast(actualType = "javax.net.ssl.SSLSocketFactory") Object sslSocketFactory)  {

        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient3_builder_sslSocketFactory_onearg", caller);

        try {
            return original.invoke(
                    caller, PreDefinedSSLInfo.getTrustAllSSLSocketFactory(), PreDefinedSSLInfo.getTrustAllX509TrustManager());
        } catch (IllegalAccessException e){
            throw new RuntimeException(e);
        } catch (InvocationTargetException e){
            throw (RuntimeException) e.getCause();
        }
    }
}
