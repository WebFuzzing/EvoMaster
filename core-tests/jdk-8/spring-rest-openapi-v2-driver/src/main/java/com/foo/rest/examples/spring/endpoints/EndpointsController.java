package com.foo.rest.examples.spring.endpoints;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.constant.ConstantApplication;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

public class EndpointsController extends SpringController {

    public EndpointsController(){
        super(EndpointsApplication.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        /*
         * Used SpringFox does not generate info for TRACE, so added manually
         */
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-endpoints.json",
                null
        );
    }
}
