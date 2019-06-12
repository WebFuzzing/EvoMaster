package com.foo.rest.examples.spring.regex;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

/**
 * Created by arcuri82 on 11-Jun-19.
 */
public class RegexController extends SpringController {

    public RegexController() {
        super(RegexApplication.class);
    }

    /*
        It seems Springfox does not generate "pattern" info.
        See for example:
        https://github.com/springfox/springfox/issues/2572
     */

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-regex.json",
                null
        );
    }

}
