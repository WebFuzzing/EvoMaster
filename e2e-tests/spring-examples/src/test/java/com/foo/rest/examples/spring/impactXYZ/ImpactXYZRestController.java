package com.foo.rest.examples.spring.impactXYZ;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.List;

/**
 * created by manzh on 2020-06-09
 */
public class ImpactXYZRestController extends SpringController {

    private List<String> skip = null;

    public ImpactXYZRestController() {
        super(ImpactXYZApplication.class);
    }

    public ImpactXYZRestController(List<String> skip) {
        super(ImpactXYZApplication.class);
        this.skip = skip;
    }

    @Override
    public void resetStateOfSUT() {
        ImpactXYZRest.data.clear();
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                skip
        );
    }
}
