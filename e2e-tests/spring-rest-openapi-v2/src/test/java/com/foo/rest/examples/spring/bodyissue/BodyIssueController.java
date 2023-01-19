package com.foo.rest.examples.spring.bodyissue;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

/**
 * Created by arcuri82 on 07-Nov-18.
 */
public class BodyIssueController extends SpringController {

    public BodyIssueController() {
        super(BodyIssueApplication.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-bodyissue.json",
                null
        );
    }
}
