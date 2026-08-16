package org.evomaster.client.java.controller.problem;

import java.util.List;
import java.util.Objects;

/**
 * Declares that the SUT is a service driven by messages, described by an AsyncAPI document.
 *
 * AsyncAPI is the event-driven counterpart of OpenAPI: it describes services that talk over a
 * broker or a socket rather than over HTTP. That difference is why a driver is needed even for
 * black-box testing, which is not so for REST or GraphQL. There is no universal wire to point
 * at -- Kafka, AMQP, MQTT and WebSocket share nothing at the API level -- so something has to
 * hold a client and move the bytes, and that something is the driver.
 *
 * The document is the only thing that crosses to the core. The connection to the broker stays
 * here, in the driver, which is what keeps the core from depending on any broker library.
 */
public class AsyncApiProblem extends ProblemInfo {

    private final String schemaLocation;

    private final String schemaText;

    /**
     * @param schemaLocation where the AsyncAPI document can be fetched from: a URL, or a path
     *                       on the machine running the driver
     */
    public AsyncApiProblem(String schemaLocation) {
        this(schemaLocation, null);
    }

    private AsyncApiProblem(String schemaLocation, String schemaText) {

        if ((schemaLocation == null) == (schemaText == null)) {
            throw new IllegalArgumentException(
                    "An AsyncAPI problem needs exactly one of a schema location and a schema text");
        }

        this.schemaLocation = schemaLocation;
        this.schemaText = schemaText;
    }

    /**
     * Declare the problem with the document itself rather than somewhere to fetch it from.
     *
     * This is the common case, and the difference from OpenAPI is worth stating: a REST service
     * usually serves its own contract over HTTP, whereas a service that speaks only Kafka has
     * no endpoint to serve anything from. Its document is far more likely to be a file shipped
     * beside it.
     */
    public static AsyncApiProblem fromSchemaText(String schemaText) {
        return new AsyncApiProblem(null, Objects.requireNonNull(schemaText));
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public String getSchemaText() {
        return schemaText;
    }

    @Override
    public AsyncApiProblem withServicesToNotMock(List<ExternalService> servicesToNotMock) {
        AsyncApiProblem p = new AsyncApiProblem(this.schemaLocation, this.schemaText);
        p.servicesToNotMock.addAll(servicesToNotMock);
        return p;
    }
}
