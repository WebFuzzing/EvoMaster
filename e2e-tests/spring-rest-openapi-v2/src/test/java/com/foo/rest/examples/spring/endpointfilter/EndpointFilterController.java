package com.foo.rest.examples.spring.endpointfilter;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.endpoints.EndpointsApplication;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.Arrays;

public class EndpointFilterController extends SpringController {

    public EndpointFilterController(){
        super(EndpointsApplication.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {

        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                Arrays.asList("/api/endpointfilter/y/z/k")
        );
    }
}
