package com.foo.spring.rest.neo4j.transaction.findnode;

import com.foo.spring.rest.neo4j.Neo4jController;
import com.foo.neo4j.transaction.findnode.Neo4jTransactionFindNodeApp;

public class Neo4jTransactionFindNodeController extends Neo4jController {

    public Neo4jTransactionFindNodeController() {
        super(Neo4jTransactionFindNodeApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.foo.neo4j.transaction.findnode";
    }
}
