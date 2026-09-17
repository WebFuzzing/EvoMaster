package com.neo4j.session.findnode;

import com.neo4j.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class Neo4jSessionFindNodeApp extends SwaggerConfiguration {

    public Neo4jSessionFindNodeApp() {
        super("neo4jsessionfindnode");
    }

    public static void main(String[] args) {
        SpringApplication.run(Neo4jSessionFindNodeApp.class, args);
    }
}
