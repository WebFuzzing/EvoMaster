package com.foo.spring.rest.cassandra.findbytag;

import com.cassandra.findbytag.CassandraFindByTagApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByTagController extends CassandraController {

    public CassandraFindByTagController() {
        super(CassandraFindByTagApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.cassandra.findbytag";
    }
}