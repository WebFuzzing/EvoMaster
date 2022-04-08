package com.foo.rest.examples.spring.wiremock.http;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingAgent;

public class HttpRequestController extends SpringController {

    public HttpRequestController() {
        super(HttpRequestApplication.class);
    }
}
