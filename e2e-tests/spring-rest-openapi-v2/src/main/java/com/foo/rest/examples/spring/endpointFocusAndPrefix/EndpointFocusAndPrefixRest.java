package com.foo.rest.examples.spring.endpointFocusAndPrefix;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;

@RestController
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class EndpointFocusAndPrefixRest {

    final String petStoreURL = "https://petstore.swagger.io";

    @RequestMapping(value = "/*")
    public void method(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", petStoreURL);
        httpServletResponse.setStatus(302);
    }
}
