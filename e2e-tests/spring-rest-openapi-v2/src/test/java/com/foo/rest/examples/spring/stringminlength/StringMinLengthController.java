package com.foo.rest.examples.spring.stringminlength;

import com.foo.rest.examples.spring.SpringController;
import com.foo.rest.examples.spring.stringminlenght.StringMinLengthApplication;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

public class StringMinLengthController extends SpringController {

    public StringMinLengthController(){
        super(StringMinLengthApplication.class);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/swagger-minlength.json",
                null
        );
    }
}
