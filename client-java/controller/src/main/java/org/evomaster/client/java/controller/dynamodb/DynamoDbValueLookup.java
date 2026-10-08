package org.evomaster.client.java.controller.dynamodb;

/**
 * Result of resolving a DynamoDB document path against a normalized item.
 * <p>
 * A separate {@link #found} flag is necessary because {@link #value} can legitimately be
 * {@code null} when the path exists and points to a DynamoDB null attribute.
 */
public final class DynamoDbValueLookup {

    public final boolean found;
    public final Object value;

    DynamoDbValueLookup(boolean found, Object value) {
        this.found = found;
        this.value = value;
    }
}
