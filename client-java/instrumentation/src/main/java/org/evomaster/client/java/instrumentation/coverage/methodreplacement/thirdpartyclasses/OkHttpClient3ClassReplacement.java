package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import okhttp3.OkHttpClient;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ReplacementList;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ThirdPartyMethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.util.List;
import java.util.stream.Collectors;

public class OkHttpClient3ClassReplacement extends ThirdPartyMethodReplacementClass {

    private static ThreadLocal<OkHttpClient> instance = new ThreadLocal<>();

    private static final OkHttpClient3ClassReplacement singleton = new OkHttpClient3ClassReplacement();

    @Override
    protected String getNameOfThirdPartyTargetClass() {
        return "okhttp3.OkHttpClient";
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

        //Constructor original = getOriginalConstructor(singleton, "okhttpclient_constructor");

       // try {
            if (!OkHttpClient3BuilderClassReplacement.hasInstance())
                OkHttpClient3BuilderClassReplacement.Builder();

            //OkHttpClient client = (OkHttpClient) original.newInstance(OkHttpClientBuilderClassReplacement.consumeInstance());
            OkHttpClient.Builder builder = OkHttpClient3BuilderClassReplacement.consumeInstance();

            addInstance(builder.build());
//        } catch (InstantiationException | IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw (RuntimeException) e.getCause();
//        }

    }
}
