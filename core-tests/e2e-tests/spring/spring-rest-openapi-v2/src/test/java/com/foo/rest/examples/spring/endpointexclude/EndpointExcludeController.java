package com.foo.rest.examples.spring.endpointexclude;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.Arrays;

public class EndpointExcludeController extends SpringController {

    public EndpointExcludeController(){
        super(EndpointExcludeApplication.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {

        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                null
        );
    }
}
