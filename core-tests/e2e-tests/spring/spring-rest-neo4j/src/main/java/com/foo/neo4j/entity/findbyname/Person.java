package com.foo.neo4j.entity.findbyname;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * The node as the SUT maps it. The mapping is what tells EvoMaster which properties a Person has,
 * beyond the one its query compares.
 */
@Node("Person")
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private Integer age;

    private Boolean active;

    private Double score;

    public Person() {
    }

    public Person(String name, Integer age, Boolean active, Double score) {
        this.name = name;
        this.age = age;
        this.active = active;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public Boolean getActive() {
        return active;
    }

    public Double getScore() {
        return score;
    }
}
