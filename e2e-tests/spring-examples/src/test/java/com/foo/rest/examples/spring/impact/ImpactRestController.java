package com.foo.rest.examples.spring.impact;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.List;

/**
 * created by manzh on 2019-09-12
 */
public class ImpactRestController extends SpringController {

    private List<String> skip = null;

    public ImpactRestController() {
        super(ImpactApplication.class);
    }

    public ImpactRestController(List<String> skip) {
        super(ImpactApplication.class);
        this.skip = skip;
    }

    @Override
    public void resetStateOfSUT() {
        ImpactRest.data.clear();
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                skip
        );
    }
}