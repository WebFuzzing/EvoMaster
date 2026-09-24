package com.foo.spring.rest.cassandra.findbyage;

import com.cassandra.findbyage.CassandraFindByAgeApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByAgeController extends CassandraController {

    public CassandraFindByAgeController() {
        super(CassandraFindByAgeApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.cassandra.findbyage";
    }
}