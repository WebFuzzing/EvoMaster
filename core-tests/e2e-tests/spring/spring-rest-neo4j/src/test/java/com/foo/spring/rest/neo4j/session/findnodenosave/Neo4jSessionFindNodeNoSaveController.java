package com.foo.spring.rest.neo4j.session.findnodenosave;

import com.foo.spring.rest.neo4j.Neo4jController;
import com.foo.neo4j.session.findnodenosave.Neo4jSessionFindNodeNoSaveApp;

public class Neo4jSessionFindNodeNoSaveController extends Neo4jController {

    public Neo4jSessionFindNodeNoSaveController() {
        super(Neo4jSessionFindNodeNoSaveApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.neo4j.session.findnodenosave";
    }
}
