package com.dynamodb.collections;

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
 * Spring application used to exercise DynamoDB collection, document, set, and null insertions end to end.
 */
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan({"com.dynamodb.config", "com.dynamodb.collections"})
public class PlayerCollectionsApp {

    public static void main(String[] args) {
        SpringApplication.run(PlayerCollectionsApp.class, args);
    }

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
