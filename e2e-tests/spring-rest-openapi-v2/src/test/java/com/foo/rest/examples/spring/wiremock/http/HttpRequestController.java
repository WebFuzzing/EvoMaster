package com.foo.rest.examples.spring.wiremock.http;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingAgent;

public class HttpRequestController extends SpringController {

    // experiment
    static {
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,NET");
        InstrumentingAgent.changePackagesToInstrument("com.foo.");
    }

    public HttpRequestController() {
        super(HttpRequestApplication.class);
    }
}
