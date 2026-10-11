package com.foo.asyncapi.shop;

/**
 * How urgent an order is. The contract carries it as a plain string, which the service reads
 * with {@link #valueOf(String)}.
 */
public enum Priority {
    LOW,
    NORMAL,
    URGENT
}
