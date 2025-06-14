package com.opensearch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {
    private int port = 9200;
    private String indexName = "default-index";

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getIndexName() { return indexName; }
    public void setIndexName(String indexName) { this.indexName = indexName; }
}