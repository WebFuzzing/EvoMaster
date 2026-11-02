package com.foo.somedifferentpackage.examples.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node
public class PlayerNode {

    @Id
    private Integer id;

    private Integer rating;

    @Relationship(type = "HAS_USER")
    private UserNode user;

    @Relationship(type = "PLAYED", direction = Relationship.Direction.INCOMING)
    private List<PlayedRelationship> matches;

    @Relationship("KNOWS")
    private List<PlayerNode> friends;
}
