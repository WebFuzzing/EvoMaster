package com.foo.rest.examples.spring.hypermutation;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.List;

public class HighWeightRestController extends SpringController {


    private List<String> skip = null;

    public HighWeightRestController() {
        super(HighWeightApplication.class);
    }

    public HighWeightRestController(List<String> skip) {
        super(HighWeightApplication.class);
        this.skip = skip;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem("http://localhost:" + getSutPort() + "/v2/api-docs", skip);
    }
}
