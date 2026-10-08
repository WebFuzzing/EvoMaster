package com.foo.neo4j.session.findnode;

import com.foo.neo4j.OpenApiConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Neo4jSessionFindNodeApp extends OpenApiConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Neo4jSessionFindNodeApp.class, args);
    }
}
