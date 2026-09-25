package com.foo.spring.rest.cassandra.findbyuuid;

import com.cassandra.findbyuuid.CassandraFindByUuidApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByUuidController extends CassandraController {

    public CassandraFindByUuidController() {
        super(CassandraFindByUuidApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.cassandra.findbyuuid";
    }
}