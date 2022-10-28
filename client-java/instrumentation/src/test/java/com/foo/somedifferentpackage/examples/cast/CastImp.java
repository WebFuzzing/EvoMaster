package com.foo.somedifferentpackage.examples.cast;

import com.squareup.okhttp.OkHttpClient;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.OkHttpClientClassReplacement;
import org.evomaster.client.java.instrumentation.example.cast.Cast;

public class CastImp implements Cast {
    @Override
    public OkHttpClient get() {
        return new OkHttpClient();
    }

    private OkHttpClient inst(){
        OkHttpClientClassReplacement.OkHttpClient();
        return (OkHttpClient) OkHttpClientClassReplacement.consumeInstance();
    }
}
