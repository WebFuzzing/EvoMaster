package com.foo.rest.examples.spring.formparam;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

public class FormParamIssueController extends SpringController {
    public FormParamIssueController() {
        super(FormParamApplication.class);
    }

    /*
        Here, in the schema we removed the

        "consumes":["application/x-www-form-urlencoded"]
     */

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-formparam-issue.json",
                null
        );
    }
}
