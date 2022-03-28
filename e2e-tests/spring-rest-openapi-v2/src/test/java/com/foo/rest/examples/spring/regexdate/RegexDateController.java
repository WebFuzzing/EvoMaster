package com.foo.rest.examples.spring.regexdate;

import com.foo.rest.examples.spring.SpringController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

/**
 * Created by arcuri82 on 27-Jun-19.
 */
public class RegexDateController extends SpringController {

    public RegexDateController() {
        super(RegexDateApplication.class);
    }

    /*
        It seems Springfox does not generate "pattern" info.
        See for example:
        https://github.com/springfox/springfox/issues/2572
     */

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-regexdate.json",
                null
        );
    }

}
