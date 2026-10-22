package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records that the supplier answered, but not with 200. Only ever reached when something answers
 * at the supplier's address at all, which takes a mock: the hostname does not resolve.
 */
public final class RestockRefused {

    private static final Logger log = LoggerFactory.getLogger(RestockRefused.class);

    private RestockRefused() {
    }

    static void record(String sku, int status) {
        log.debug("Supplier answered {} to a restock of {}", status, sku);
    }
}
