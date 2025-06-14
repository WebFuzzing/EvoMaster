package com.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;

@Repository
public class OpenSearchRepository {

    @Autowired
    private OpenSearchClient openSearchClient;

    @Autowired
    private OpenSearchProperties openSearchProperties;

    public <T> T getById(String id, Class<T> clazz) throws IOException {
        GetRequest getRequest = new GetRequest.Builder()
            .index(openSearchProperties.getIndexName())
            .id(id)
            .build();

        GetResponse<T> response = openSearchClient.get(getRequest, clazz);
        return response.found() ? response.source() : null;
    }
}