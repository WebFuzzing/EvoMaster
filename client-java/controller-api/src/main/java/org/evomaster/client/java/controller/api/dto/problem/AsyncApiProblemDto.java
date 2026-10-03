package org.evomaster.client.java.controller.api.dto.problem;

/**
 * Info the driver gives about an AsyncAPI service, so that the core can learn what the service
 * consumes and what shape those messages have.
 *
 * Note the transport client is not here, and never crosses: it is an open connection to a
 * broker, held by the driver. Only the document travels.
 */
public class AsyncApiProblemDto extends ProblemInfoDto {

    /**
     * Where the AsyncAPI document can be fetched from: a URL, or a path on the machine running
     * the driver. Null when the document is given inline instead.
     */
    public String schemaLocation;

    /**
     * The AsyncAPI document itself. Null when a location is given instead.
     *
     * Useful when the document is packaged with the service rather than served by it, which is
     * the common case: unlike OpenAPI, an AsyncAPI service rarely exposes its own contract over
     * HTTP, since it may not speak HTTP at all.
     */
    public String schemaText;
}
