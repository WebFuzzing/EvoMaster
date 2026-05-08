package org.evomaster.client.java.controller.problem;

import java.util.List;

/**
 * Driver-side description of an AsyncAPI 3.0 SUT.
 *
 * Embedded/External controllers return this from {@code getProblemInfo()} when
 * the SUT speaks AsyncAPI rather than REST/GraphQL/RPC. The core then knows it
 * needs to load the schema, talk to the broker, and run the AsyncAPI search
 * engine.
 *
 * Either {@link #getAsyncApiUrl()} or {@link #getAsyncApiSchema()} must be
 * non-null; the validating constructor enforces the same rule
 * {@link RestProblem} does.
 */
public class AsyncAPIProblem extends ProblemInfo {

    /**
     * Default Kafka header name used to correlate request/reply pairs. Drivers
     * typically use this value; the AsyncAPI schema declares it via
     * {@code correlationId.location}.
     */
    public static final String DEFAULT_CORRELATION_HEADER = "evm-correlation-id";

    private final String asyncApiUrl;
    private final String asyncApiSchema;
    private final String brokerBootstrapServers;
    private final String correlationIdHeader;

    public AsyncAPIProblem(String asyncApiUrl, String brokerBootstrapServers) {
        this(asyncApiUrl, null, brokerBootstrapServers, DEFAULT_CORRELATION_HEADER, null);
    }

    public AsyncAPIProblem(
            String asyncApiUrl,
            String asyncApiSchema,
            String brokerBootstrapServers,
            String correlationIdHeader
    ) {
        this(asyncApiUrl, asyncApiSchema, brokerBootstrapServers, correlationIdHeader, null);
    }

    /**
     * @param asyncApiUrl URL where the AsyncAPI 3.0 schema can be downloaded
     *                    from (e.g. {@code http://localhost:8080/asyncapi.yaml}).
     *                    Mutually exclusive with {@code asyncApiSchema}.
     * @param asyncApiSchema The schema text inline. Set this only when no URL
     *                       is available.
     * @param brokerBootstrapServers Broker bootstrap URL (Kafka:
     *                               {@code host:port}; comma-separated for
     *                               multiple brokers).
     * @param correlationIdHeader Header carrying request/reply correlation id.
     *                            Defaults to {@link #DEFAULT_CORRELATION_HEADER}
     *                            when null.
     * @param servicesToNotMock external services the search must not mock
     *                          (mirrors the {@link RestProblem} contract).
     */
    public AsyncAPIProblem(
            String asyncApiUrl,
            String asyncApiSchema,
            String brokerBootstrapServers,
            String correlationIdHeader,
            List<ExternalService> servicesToNotMock
    ) {
        boolean url = asyncApiUrl != null && !asyncApiUrl.isEmpty();
        boolean schema = asyncApiSchema != null && !asyncApiSchema.isEmpty();
        if (!url && !schema) {
            throw new IllegalArgumentException("MUST either provide a URL or a full schema for AsyncAPI");
        }
        if (brokerBootstrapServers == null || brokerBootstrapServers.isEmpty()) {
            throw new IllegalArgumentException("brokerBootstrapServers is required for AsyncAPI");
        }

        this.asyncApiUrl = asyncApiUrl;
        this.asyncApiSchema = asyncApiSchema;
        this.brokerBootstrapServers = brokerBootstrapServers;
        this.correlationIdHeader = (correlationIdHeader == null || correlationIdHeader.isEmpty())
                ? DEFAULT_CORRELATION_HEADER
                : correlationIdHeader;

        this.servicesToNotMock.clear();
        if (servicesToNotMock != null && !servicesToNotMock.isEmpty()) {
            this.servicesToNotMock.addAll(servicesToNotMock);
        }
    }

    @Override
    public AsyncAPIProblem withServicesToNotMock(List<ExternalService> servicesToNotMock) {
        return new AsyncAPIProblem(
                asyncApiUrl, asyncApiSchema, brokerBootstrapServers, correlationIdHeader, servicesToNotMock
        );
    }

    public String getAsyncApiUrl() {
        return asyncApiUrl;
    }

    public String getAsyncApiSchema() {
        return asyncApiSchema;
    }

    public String getBrokerBootstrapServers() {
        return brokerBootstrapServers;
    }

    public String getCorrelationIdHeader() {
        return correlationIdHeader;
    }
}
