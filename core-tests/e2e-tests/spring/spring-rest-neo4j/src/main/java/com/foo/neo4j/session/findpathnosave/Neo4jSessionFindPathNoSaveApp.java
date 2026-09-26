package com.foo.neo4j.session.findpathnosave;

import com.foo.neo4j.OpenApiConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Neo4jSessionFindPathNoSaveApp extends OpenApiConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Neo4jSessionFindPathNoSaveApp.class, args);
    }
}
