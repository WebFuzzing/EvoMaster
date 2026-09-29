package com.foo.spring.rest.cassandra.findbytag;

import com.foo.cassandra.findbytag.CassandraFindByTagApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByTagController extends CassandraController {

    public CassandraFindByTagController() {
        super(CassandraFindByTagApp.class);
    }
}