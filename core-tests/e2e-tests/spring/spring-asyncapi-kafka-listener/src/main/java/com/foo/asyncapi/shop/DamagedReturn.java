package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Refunds a return at once, without waiting for the items to arrive, because they came damaged.
 * Only ever reached from {@link ReturnListener#onReturnRequested}, when the reason is
 * {@link ReturnListener#DAMAGED}, so whether any of its code is covered says whether a search got
 * there.
 */
public final class DamagedReturn {

    private static final Logger log = LoggerFactory.getLogger(DamagedReturn.class);

    private DamagedReturn() {
    }

    static void refund(ReturnRequested request) {
        log.debug("Order {} refunded for damaged items", request.getOrderId());
    }
}
