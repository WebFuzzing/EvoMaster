package com.foo.somedifferentpackage.examples.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node(primaryLabel = "User", labels = {"Account"})
public class UserNode extends AuditedNode {

    @Id
    @GeneratedValue
    private Long id;

    private String username;
}
