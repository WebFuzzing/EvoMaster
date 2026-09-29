package com.foo.spring.rest.cassandra.findbyagereadonly;

import com.foo.cassandra.findbyagereadonly.CassandraFindByAgeReadOnlyApp;
import com.foo.spring.rest.cassandra.CassandraController;

public class CassandraFindByAgeReadOnlyController extends CassandraController {

    public CassandraFindByAgeReadOnlyController() {
        super(CassandraFindByAgeReadOnlyApp.class);
    }
}