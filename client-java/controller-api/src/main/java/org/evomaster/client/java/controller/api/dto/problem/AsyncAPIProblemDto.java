package org.evomaster.client.java.controller.api.dto.problem;

/**
 * Driver → core info for an AsyncAPI 3.0 SUT.
 *
 * Mirrors {@link RestProblemDto} but carries broker connection info because
 * AsyncAPI tests publish/subscribe rather than make HTTP calls. Either
 * {@link #asyncApiUrl} or {@link #asyncApiSchema} must be supplied; if both
 * are present the URL takes precedence so the inline copy can stay small.
 */
public class AsyncAPIProblemDto extends ProblemInfoDto {

    /**
     * Full URL of where the AsyncAPI 3.0 schema can be downloaded from.
     * Use {@code file://} for local files, or a plain HTTP(S) URL when the
     * SUT serves it over the wire (recommended pattern: a dedicated
     * controller exposing {@code GET /asyncapi.yaml}).
     */
    public String asyncApiUrl;

    /**
     * Inline AsyncAPI schema text. Set this when the schema is generated at
     * runtime and not exposed via an endpoint. If both this and
     * {@link #asyncApiUrl} are set, the URL wins.
     */
    public String asyncApiSchema;

    /**
     * Broker bootstrap servers EvoMaster will use to publish requests and
     * observe replies. For Kafka this is the {@code bootstrap.servers}
     * value, e.g. {@code "localhost:9092"}.
     */
    public String brokerBootstrapServers;

    /**
     * Header that carries the request/reply correlation id. The driver and
     * SUT must agree on this name; default matches the WFD convention used
     * in {@code rest-kafka-ncs}.
     */
    public String correlationIdHeader = "evm-correlation-id";
}
