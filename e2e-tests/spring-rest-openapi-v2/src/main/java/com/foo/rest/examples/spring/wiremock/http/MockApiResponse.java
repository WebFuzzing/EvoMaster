package com.foo.rest.examples.spring.wiremock.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MockApiResponse {
    public final String message;

    public MockApiResponse(@JsonProperty("message") String message) {
        this.message = message;
    }
}
