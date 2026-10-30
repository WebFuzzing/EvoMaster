package com.foo.asyncapi.kafka.simple;

import org.springframework.stereotype.Service;

/**
 * The two operations of the service: each replies with a constant, and fails on a negative x.
 */
@Service
public class SimpleService {

    public int foo(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("x must not be negative");
        }
        return 1;
    }

    public int bar(int x) {
        if (x < 0) {
            throw new IllegalArgumentException("x must not be negative");
        }
        return 2;
    }
}
