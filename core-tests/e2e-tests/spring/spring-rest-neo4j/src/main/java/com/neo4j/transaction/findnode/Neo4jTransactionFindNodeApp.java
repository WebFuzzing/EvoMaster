package com.neo4j.transaction.findnode;

import com.neo4j.SwaggerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@SpringBootApplication
public class Neo4jTransactionFindNodeApp extends SwaggerConfiguration {

    public Neo4jTransactionFindNodeApp() {
        super("neo4jtransactionfindnode");
    }

    public static void main(String[] args) {
        SpringApplication.run(Neo4jTransactionFindNodeApp.class, args);
    }
}
