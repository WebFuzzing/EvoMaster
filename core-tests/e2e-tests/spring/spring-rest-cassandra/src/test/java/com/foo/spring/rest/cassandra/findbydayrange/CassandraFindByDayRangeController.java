package com.foo.spring.rest.cassandra.findbydayrange;

import com.foo.cassandra.findbydayrange.CassandraFindByDayRangeApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByDayRangeController extends CassandraController {

    public CassandraFindByDayRangeController() {
        super(CassandraFindByDayRangeApp.class);
    }
}