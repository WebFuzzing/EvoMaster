package com.foo.neo4j.session.findnodenosave;

import com.foo.neo4j.OpenApiConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Neo4jSessionFindNodeNoSaveApp extends OpenApiConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Neo4jSessionFindNodeNoSaveApp.class, args);
    }
}
