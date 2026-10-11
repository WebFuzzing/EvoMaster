package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records that more items of a product were taken than there were in stock. Only ever reached
 * from {@link StockLedger#adjust}, for a product an earlier message recorded, so whether any of
 * its code is covered says whether a test sent the messages that lead there.
 */
public final class OversoldStock {

    private static final Logger log = LoggerFactory.getLogger(OversoldStock.class);

    private OversoldStock() {
    }

    static void record(String sku, int quantity) {
        log.debug("Product {} oversold, stock at {}", sku, quantity);
    }
}
