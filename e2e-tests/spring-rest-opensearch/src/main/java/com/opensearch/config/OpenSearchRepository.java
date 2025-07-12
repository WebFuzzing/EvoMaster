package com.opensearch.config;

import java.util.List;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
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

    public <T> List<T> search(String q, Class<T> clazz) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
            .index(openSearchProperties.getIndexName())
            .q(wrapIntoDoubleQuotes(q))
            // query example which matches 2 random field names with query string random
            .query(
                query -> query
                    .match(matcher -> matcher.field("title").query(value -> value.stringValue(q)))
            )
            .build();

        SearchResponse<T> response = openSearchClient.search(searchRequest, clazz);
        return !response.documents().isEmpty() ? response.documents() : null;
    }

    private static String wrapIntoDoubleQuotes(String q) {
        return "\"" + q + "\"";
    }
}