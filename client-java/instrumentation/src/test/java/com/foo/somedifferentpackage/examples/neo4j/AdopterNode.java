package com.foo.somedifferentpackage.examples.neo4j;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDate;
import java.util.List;

@Node("Adopter")
public class AdopterNode {

    public static final String IGNORED = "static";

    @Id
    private String id;

    private String name;

    private Integer budget;

    private boolean hasYard;

    @Property("max_dogs")
    private long maxDogs;

    private Double score;

    private Level level;

    private List<String> tags;

    private LocalDate since;

    @Transient
    private String notStored;

    public enum Level { BRONZE, GOLD }
}
