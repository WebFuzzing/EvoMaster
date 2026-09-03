package com.dynamodb.players;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;

/**
 * Spring application used to exercise DynamoDB query heuristics end to end.
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan({"com.dynamodb.config", "com.dynamodb.players"})
public class WorldCupPlayersApp {

    /**
     * Starts the World Cup players application.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(WorldCupPlayersApp.class, args);
    }

    /**
     * Creates the asynchronous client used by the SUT.
     *
     * @param endpoint DynamoDB Local endpoint
     * @return configured DynamoDB client
     */
    @Bean(destroyMethod = "close")
    public DynamoDbAsyncClient dynamoDbClient(@Value("${dynamodb.endpoint}") String endpoint) {
        return DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("local", "local")))
                .build();
    }
}
