package com.foo.rest.examples.spring.expectations;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

public class ExpectationsController extends SpringController {

    public ExpectationsController(){
        super(ExpectationsSpringRest.class);
    }

    @Override
    public ProblemInfo getProblemInfo(){
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-expectation-test.json",
                null
        );
    }
}
