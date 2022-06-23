package com.foo.rest.examples.spring.db.directintwithsql;

import com.foo.rest.examples.spring.db.directint.DbDirectIntController;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.util.Arrays;
import java.util.Collections;

public class DbDirectIntWithSqlController extends DbDirectIntController {

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                //avoid using POST that would create the data
                Collections.singletonList("/api/db/directint")
        );
    }
}
