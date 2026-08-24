package com.webfuzzing.asyncapi.models;

/**
 * An entry under {@code components.securitySchemes}, i.e. how a client authenticates to the
 * broker.
 *
 * {@link #getType()} is a free-form string rather than an enum on purpose: AsyncAPI's catalogue
 * is broad and broker-specific ({@code userPassword}, {@code scramSha512}, {@code gssapi},
 * {@code X509}, {@code oauth2}, ...), and parsing must not fail on a scheme that is perfectly
 * valid but that no transport here can use yet. Whether a scheme can actually be honoured is
 * the connecting client's question.
 */
public class AsyncApiSecurityScheme {

    private final String name;

    private final String type;

    private final String location;

    private final String scheme;

    private final String bearerFormat;

    private final String description;

    public AsyncApiSecurityScheme(
            String name,
            String type,
            String location,
            String scheme,
            String bearerFormat,
            String description) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.scheme = scheme;
        this.bearerFormat = bearerFormat;
        this.description = description;
    }

    /**
     * The component key, or a synthetic name when the scheme was written inline where it is
     * used.
     */
    public String getName() {
        return name;
    }

    /**
     * Lowercased, as real documents write both "X509" and "x509".
     */
    public String getType() {
        return type;
    }

    /**
     * For {@code apiKey} and {@code httpApiKey}: where the key travels ("header", "query",
     * "user", "password"). This is the document's {@code in} field, which is a Java keyword.
     */
    public String getLocation() {
        return location;
    }

    /**
     * For {@code http}: "basic", "bearer", and so on.
     */
    public String getScheme() {
        return scheme;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public String getDescription() {
        return description;
    }
}
