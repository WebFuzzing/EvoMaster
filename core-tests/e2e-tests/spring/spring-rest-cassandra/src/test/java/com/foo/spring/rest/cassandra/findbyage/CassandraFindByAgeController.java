package com.foo.spring.rest.cassandra.findbyage;

import com.foo.cassandra.findbyage.CassandraFindByAgeApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByAgeController extends CassandraController {

    public CassandraFindByAgeController() {
        super(CassandraFindByAgeApp.class);
    }
}