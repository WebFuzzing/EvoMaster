package com.foo.spring.rest.opensearch;

import com.opensearch.config.SampleClient;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

public abstract class OpenSearchController extends EmbeddedSutController {

    private static final int OPENSEARCH_PORT = 9200;
    private OpenSearchClient openSearchClient;

    private final GenericContainer<?> opensearch = new OpenSearchContainer<>("opensearchproject/opensearch:latest")
            .withExposedPorts(OPENSEARCH_PORT);
    private ConfigurableApplicationContext ctx;

    private final String indexName;

    private final Class<?> opensearchAppClass;

    protected OpenSearchController(String indexName, Class<?> opensearchAppClass) {
        this.indexName = indexName;
        this.opensearchAppClass = opensearchAppClass;
        super.setControllerPort(0);
    }

    @Override
    public String startSut() {
        opensearch.start();
        int port = opensearch.getMappedPort(OPENSEARCH_PORT);

        try {
            openSearchClient = SampleClient.create(port);
            openSearchClient.indices().create(CreateIndexRequest.builder().index(indexName).build());
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | IOException e) {
            throw new RuntimeException("Failed to create OpenSearch client", e);
        }

        SpringApplicationBuilder app = new SpringApplicationBuilder(opensearchAppClass);
        app.properties(
            "--server.port=0",
            "opensearch.port=" + port,
            "opensearch.indexName=" + indexName
        );

        ctx = app.run();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();
        opensearch.stop();
    }

    @Override
    public void resetStateOfSUT() {
//        try {
//            openSearchClient.indices().delete(DeleteIndexRequest.builder().index(indexName).build());
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return null;
    }

    protected int getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

//    @Override
//    public OpenSearchClient getOpenSearchConnection() {
//        return openSearchClient;
//    }
}
