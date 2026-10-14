package com.mongolegacy.persons;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongolegacy.SwaggerConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * SUT using directly the legacy MongoDB driver 3.8 (no Spring Data).
 * Spring Boot auto-configuration for Mongo is excluded, as it targets 4.x drivers.
 */
@EnableSwagger2
@SpringBootApplication(exclude = MongoAutoConfiguration.class)
public class LegacyMongoPersonsApp extends SwaggerConfiguration {

    public LegacyMongoPersonsApp() {
        super("legacymongo");
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(@Value("${spring.data.mongodb.host}") String host,
                                   @Value("${spring.data.mongodb.port}") int port) {
        return MongoClients.create("mongodb://" + host + ":" + port);
    }

    public static void main(String[] args) {
        SpringApplication.run(LegacyMongoPersonsApp.class, args);
    }
}
