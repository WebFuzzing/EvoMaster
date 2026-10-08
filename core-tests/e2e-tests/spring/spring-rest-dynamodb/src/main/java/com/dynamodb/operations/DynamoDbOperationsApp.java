package com.dynamodb.operations;

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
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Spring application exercising every supported DynamoDB operation and client variant.
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan({"com.dynamodb.config", "com.dynamodb.operations"})
public class DynamoDbOperationsApp {

    /**
     * Starts the DynamoDB operations application.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DynamoDbOperationsApp.class, args);
    }

    /**
     * Creates the synchronous SDK client used by the SUT.
     *
     * @param endpoint DynamoDB Local endpoint
     * @return configured synchronous client
     */
    @Bean(destroyMethod = "close")
    public DynamoDbClient dynamoDbClient(@Value("${dynamodb.endpoint}") String endpoint) {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * Creates the asynchronous SDK client used by the SUT.
     *
     * @param endpoint DynamoDB Local endpoint
     * @return configured asynchronous client
     */
    @Bean(destroyMethod = "close")
    public DynamoDbAsyncClient dynamoDbAsyncClient(@Value("${dynamodb.endpoint}") String endpoint) {
        return DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * Creates deterministic credentials accepted by DynamoDB Local.
     *
     * @return static local credentials provider
     */
    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local"));
    }
}
