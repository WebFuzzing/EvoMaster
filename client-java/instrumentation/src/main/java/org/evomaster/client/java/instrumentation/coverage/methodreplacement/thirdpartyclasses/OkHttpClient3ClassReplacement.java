package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils.collectExternalServiceInfo;

public class OkHttpClient3ClassReplacement extends ThirdPartyMethodReplacementClass {

    private static ThreadLocal<Object> instance = new ThreadLocal<>();

    private static final OkHttpClient3ClassReplacement singleton = new OkHttpClient3ClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "okhttp3.OkHttpClient";
    }


    public static Object consumeInstance(){

        OkHttpClient client = (OkHttpClient) instance.get();
        if(client == null){
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return client;
    }

    private static void addInstance(OkHttpClient x){
        OkHttpClient client = (OkHttpClient) instance.get();
        if(client != null){
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient3_constructor",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            replacingConstructor = true,
            castTo = "okhttp3.OkHttpClient"
    )
    public static void OkHttpClient()  {

        //Constructor original = getOriginalConstructor(singleton, "okhttpclient_constructor");

       // try {
            if (!OkHttpClient3BuilderClassReplacement.hasInstance())
                OkHttpClient3BuilderClassReplacement.Builder();

            //OkHttpClient client = (OkHttpClient) original.newInstance(OkHttpClientBuilderClassReplacement.consumeInstance());
            OkHttpClient.Builder builder = (OkHttpClient.Builder) OkHttpClient3BuilderClassReplacement.consumeInstance();

            addInstance(builder.build());
//        } catch (InstantiationException | IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw (RuntimeException) e.getCause();
//        }

    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "okhttpclient3_newCall",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            castTo = "okhttp3.Call"
    )
    public static Object newCall(Object caller, Request request){
        if(caller == null){
            throw new NullPointerException();
        }

        Method original = getOriginal(singleton, "okhttpclient3_newCall", caller);
        Request replaced = request;

        HttpUrl url = request.url();
        if (url.scheme().equalsIgnoreCase("https") || url.scheme().equalsIgnoreCase("http")){
            ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(url.scheme(), url.host(), url.port());
            String[] ipAndPort = collectExternalServiceInfo(remoteHostInfo, url.port());
            replaced = new Request.Builder().url(url.scheme()+"://"+ipAndPort[0]+":"+ipAndPort[1]+url.encodedPath()).build();
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
