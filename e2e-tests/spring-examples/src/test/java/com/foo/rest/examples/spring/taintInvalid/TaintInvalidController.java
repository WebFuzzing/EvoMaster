package com.foo.rest.examples.spring.taintInvalid;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.taint.TaintApplication;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.Arrays;

/**
 * Created by arcuri82 on 06-Sep-19.
 */
public class TaintInvalidController extends SpringController {

    public TaintInvalidController(){
        super(TaintInvalidApplication.class);
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                Arrays.asList("/error")
        );
    }
}
