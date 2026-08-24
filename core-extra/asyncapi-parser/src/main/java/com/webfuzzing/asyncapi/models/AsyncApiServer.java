package com.webfuzzing.asyncapi.models;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An entry under {@code servers:}, i.e. one broker the API is exposed on.
 *
 * A document is not required to declare any server, and many published ones do not: they
 * describe the message contract and leave the address to deployment.
 */
public class AsyncApiServer {

    private final String name;

    private final String host;

    private final String protocol;

    private final String protocolVersion;

    private final String pathname;

    /**
     * Key is the variable name, as used by a {@code {placeholder}} in the host or pathname.
     * Value is that variable's declaration.
     */
    private final Map<String, AsyncApiServerVariable> variables;

    /**
     * Names of the security schemes this server requires, as keys into
     * {@link AsyncApiDocument#getSecuritySchemes()}.
     */
    private final List<String> security;

    private AsyncApiServer(Builder builder) {
        this.name = builder.name;
        this.host = builder.host;
        this.protocol = builder.protocol;
        this.protocolVersion = builder.protocolVersion;
        this.pathname = builder.pathname;
        this.variables = Collections.unmodifiableMap(builder.variables);
        this.security = Collections.unmodifiableList(builder.security);
    }

    public static Builder builder(String name, String host, String protocol) {
        return new Builder(name, host, protocol);
    }

    public String getName() {
        return name;
    }

    /**
     * Host and optional port, e.g. "localhost:9092".
     */
    public String getHost() {
        return host;
    }

    /**
     * Wire protocol, e.g. "kafka", "amqp", "mqtt", "ws".
     *
     * This is also how AsyncAPI distinguishes the two incompatible AMQPs: "amqp" is 0-9-1,
     * while 1.0 is the separate "amqp1" protocol.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Free-text and rarely set, so a client that needs to know the dialect has to fall back on
     * a conservative default -- MQTT 3.1.1 rather than 5.0, for instance.
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Path prefix, e.g. "everest_api/1/error_history_consumer/{module_id}", for the transports
     * whose address space has paths. Kept verbatim: substituting the placeholders is a run-time
     * concern.
     */
    public String getPathname() {
        return pathname;
    }

    /**
     * What the {@code {placeholders}} in {@link #getHost()} and {@link #getPathname()} refer to.
     */
    public Map<String, AsyncApiServerVariable> getVariables() {
        return variables;
    }

    /**
     * Names of the security schemes this server requires.
     */
    public List<String> getSecurity() {
        return security;
    }

    public static class Builder {

        private final String name;
        private final String host;
        private final String protocol;
        private String protocolVersion;
        private String pathname;
        /** @see AsyncApiServer#variables */
        private Map<String, AsyncApiServerVariable> variables = Collections.emptyMap();
        /** @see AsyncApiServer#security */
        private List<String> security = Collections.emptyList();

        private Builder(String name, String host, String protocol) {
            this.name = name;
            this.host = host;
            this.protocol = protocol;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder pathname(String pathname) { this.pathname = pathname; return this; }

        public Builder variables(Map<String, AsyncApiServerVariable> variables) {
            this.variables = variables;
            return this;
        }

        public Builder security(List<String> security) { this.security = security; return this; }

        public AsyncApiServer build() {
            return new AsyncApiServer(this);
        }
    }
}
