package com.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {
    @Bean
    public OpenSearchClient openSearchClient() throws Exception {
        return SampleClient.create(9200);
    }
}