package org.evomaster.client.java.instrumentation.object.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class DtoNaming {

    @SerializedName("foo_bar")
    private String fooBar;

    @JsonProperty("hello_world")
    private String helloWorld;

    @JsonProperty("")
    private String foo;


    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public String getFooBar() {
        return fooBar;
    }

    public void setFooBar(String fooBar) {
        this.fooBar = fooBar;
    }

    public String getHelloWorld() {
        return helloWorld;
    }

    public void setHelloWorld(String helloWorld) {
        this.helloWorld = helloWorld;
    }
}
