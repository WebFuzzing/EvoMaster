package com.foo.rest.examples.spring.db.directintwithsql;

import com.foo.rest.examples.spring.db.directint.DbDirectIntController;
import org.evomaster.clientJava.controller.problem.ProblemInfo;
import org.evomaster.clientJava.controller.problem.RestProblem;

import java.util.Arrays;
import java.util.List;

public class DbDirectIntWithSqlController extends DbDirectIntController {

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                //avoid using POST that would create the data
                Arrays.asList("/api/db/directint")
        );
    }
}
