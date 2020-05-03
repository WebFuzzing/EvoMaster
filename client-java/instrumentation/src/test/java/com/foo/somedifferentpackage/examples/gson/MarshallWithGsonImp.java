package com.foo.somedifferentpackage.examples.gson;

import com.google.gson.Gson;
import org.evomaster.client.java.instrumentation.example.gson.FooBar;
import org.evomaster.client.java.instrumentation.example.gson.MarshallWithGson;

public class MarshallWithGsonImp implements MarshallWithGson {


    @Override
    public Object doMarshall(String json) {
        Gson GSON = new Gson();
        return GSON.fromJson(json, FooBar.class);
    }
}
