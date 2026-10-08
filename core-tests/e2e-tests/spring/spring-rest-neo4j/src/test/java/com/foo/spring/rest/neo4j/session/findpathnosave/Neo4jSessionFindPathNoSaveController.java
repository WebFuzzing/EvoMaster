package com.foo.spring.rest.neo4j.session.findpathnosave;

import com.foo.spring.rest.neo4j.Neo4jController;
import com.foo.neo4j.session.findpathnosave.Neo4jSessionFindPathNoSaveApp;

public class Neo4jSessionFindPathNoSaveController extends Neo4jController {

    public Neo4jSessionFindPathNoSaveController() {
        super(Neo4jSessionFindPathNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.neo4j.session.findpathnosave";
    }
}
