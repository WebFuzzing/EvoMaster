package org.evomaster.clientJava.instrumentation.example.queryparam;

import org.springframework.web.context.request.WebRequest;

public interface UsingWebRequest {


    String compute(WebRequest webRequest);
}
