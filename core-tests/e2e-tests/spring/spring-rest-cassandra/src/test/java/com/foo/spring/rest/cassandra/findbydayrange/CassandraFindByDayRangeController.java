package com.foo.spring.rest.cassandra.findbydayrange;

import com.cassandra.findbydayrange.CassandraFindByDayRangeApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByDayRangeController extends CassandraController {

    public CassandraFindByDayRangeController() {
        super(CassandraFindByDayRangeApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.cassandra.findbydayrange";
    }
}