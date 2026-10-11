package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records that the supplier accepted a restock. Only ever reached when the supplier answered
 * 200, which the real one, at a hostname that does not resolve, never does: whether any of its
 * code is covered says whether the supplier was mocked, and answered what the search chose.
 */
public final class RestockConfirmed {

    private static final Logger log = LoggerFactory.getLogger(RestockConfirmed.class);

    private RestockConfirmed() {
    }

    static void record(String sku, int quantity) {
        log.debug("Supplier confirmed {} more of {}", quantity, sku);
    }
}
