package com.foo.neo4j.entity.findbyname;

import com.foo.neo4j.OpenApiConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration;

/**
 * Only the mapping annotations of Spring Data are used here; the queries go through the driver.
 */
@SpringBootApplication(exclude = {
        Neo4jAutoConfiguration.class,
        Neo4jDataAutoConfiguration.class,
        Neo4jRepositoriesAutoConfiguration.class
})
public class Neo4jEntityFindByNameApp extends OpenApiConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(Neo4jEntityFindByNameApp.class, args);
    }
}
