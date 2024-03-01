package com.foo.rest.examples.spring.wiremock.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MockApiResponse {

    public final String message;

    public MockApiResponse(@JsonProperty("message") String message) {
        this.message = message;
    }
}
