package com.foo.spring.rest.neo4j.session.findnode;

import com.foo.spring.rest.neo4j.Neo4jController;
import com.neo4j.session.findnode.Neo4jSessionFindNodeApp;

public class Neo4jSessionFindNodeController extends Neo4jController {

    public Neo4jSessionFindNodeController() {
        super(Neo4jSessionFindNodeApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.neo4j.session.findnode";
    }
}
