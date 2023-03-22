package com.foo.rest.examples.spring.wiremock.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DummyResponse {

    public final String message;

    public DummyResponse(@JsonProperty("message") String message) {
        this.message = message;
    }
}
