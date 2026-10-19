package com.foo.somedifferentpackage.examples.neo4j;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class PlayedRelationship {

    private Integer score;

    @TargetNode
    private AdopterNode match;
}
