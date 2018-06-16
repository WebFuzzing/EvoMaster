package com.foo.rest.examples.spring.db.directintwithsql;

import com.foo.rest.examples.spring.db.directint.DbDirectIntController;

import java.util.Arrays;
import java.util.List;

public class DbDirectIntWithSqlController extends DbDirectIntController {

    @Override
    public List<String> getEndpointsToSkip(){
        //avoid using POST that would create the data
        return Arrays.asList("/api/db/directint");
    }
}
