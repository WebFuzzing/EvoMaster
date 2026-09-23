package com.foo.neo4j;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public abstract class AbstractNeo4jRest {

    public static final String NEO4J_URI_PROPERTY = "neo4j.uri";

    protected Driver driver;

    @PostConstruct
    public void init() {
        String uri = System.getProperty(NEO4J_URI_PROPERTY, "bolt://localhost:7687");
        driver = GraphDatabase.driver(uri, AuthTokens.none());
    }

    @PreDestroy
    public void shutdown() {
        driver.close();
    }
}
