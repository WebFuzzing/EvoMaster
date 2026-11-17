package org.evomaster.client.java.instrumentation.dynamodb;

/**
 * Semantic formats for constrained string-backed DynamoDB attributes captured from enhanced-client table schemas.
 */
public enum DynamoDbAttributeFormat {

    /** ISO-8601 / RFC 3339 timestamp with offset or UTC {@code Z} (e.g., {@code Instant}, {@code OffsetDateTime}, {@code ZonedDateTime}). */
    DATE_TIME("date-time"),

    /** ISO-8601 local date-time without timezone offset (e.g., {@code LocalDateTime}). */
    LOCAL_DATE_TIME("local-date-time"),

    /** ISO-8601 full calendar date {@code YYYY-MM-DD} (e.g., {@code LocalDate}). */
    DATE("date"),

    /** ISO-8601 local time without timezone offset {@code HH:MM:SS[.nnnnnnnnn]} (e.g., {@code LocalTime}). */
    LOCAL_TIME("local-time"),

    /** RFC 4122 36-character UUID string (e.g., {@code UUID}). */
    UUID("uuid");

    private final String value;

    DynamoDbAttributeFormat(String value) {
        this.value = value;
    }

    /**
     * @return OpenAPI/EvoMaster format identifier corresponding to this enum constant
     */
    public String getValue() {
        return value;
    }
}
