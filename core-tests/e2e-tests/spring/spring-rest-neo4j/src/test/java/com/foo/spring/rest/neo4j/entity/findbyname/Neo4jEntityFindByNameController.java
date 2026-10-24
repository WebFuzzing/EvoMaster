package com.foo.spring.rest.neo4j.entity.findbyname;

import com.foo.neo4j.entity.findbyname.Neo4jEntityFindByNameApp;
import com.foo.spring.rest.neo4j.Neo4jController;

public class Neo4jEntityFindByNameController extends Neo4jController {

    public Neo4jEntityFindByNameController() {
        super(Neo4jEntityFindByNameApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.neo4j.entity.findbyname";
    }
}
