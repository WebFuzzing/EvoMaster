package com.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfiguration {
    @Autowired
    private OpenSearchProperties properties;

    @Bean
    public OpenSearchClient openSearchClient() throws Exception {
        return SampleClient.create(properties.getPort());
    }
}