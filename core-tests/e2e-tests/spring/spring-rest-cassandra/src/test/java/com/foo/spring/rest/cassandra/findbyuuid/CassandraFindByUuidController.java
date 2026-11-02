package com.foo.spring.rest.cassandra.findbyuuid;

import com.foo.cassandra.findbyuuid.CassandraFindByUuidApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByUuidController extends CassandraController {

    public CassandraFindByUuidController() {
        super(CassandraFindByUuidApp.class);
    }
}