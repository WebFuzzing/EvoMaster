package com.foo.rest.examples.spring.resource;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.List;

/**
 * created by manzh on 2019-08-12
 */
public class ResourceRestController extends SpringWithDbController {

    private List<String> skip = null;

    public ResourceRestController() {
        super(ResourceApplication.class);
    }

    public ResourceRestController(List<String> skip) {
        super(ResourceApplication.class);
        this.skip = skip;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                skip
        );
    }
}
