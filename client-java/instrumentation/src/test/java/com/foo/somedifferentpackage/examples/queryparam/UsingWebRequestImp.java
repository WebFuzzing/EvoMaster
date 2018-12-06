package com.foo.somedifferentpackage.examples.queryparam;

import org.evomaster.client.java.instrumentation.example.queryparam.UsingWebRequest;
import org.springframework.web.context.request.WebRequest;

public class UsingWebRequestImp implements UsingWebRequest {


    @Override
    public String compute(WebRequest webRequest) {

        String p0 = webRequest.getParameter("p0");
        String p1 = webRequest.getParameterValues("p1")[0];

        String h0 = webRequest.getHeader("h0");
        String h1 = webRequest.getHeaderValues("h1")[0];

        return "" + p0 + p1 + h0 + h1;
    }
}
