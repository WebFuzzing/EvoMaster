package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Books a courier for an order shipped express. Only ever reached from
 * {@link OrderListener#onOrderPlaced}, when the shipping is {@link OrderListener#EXPRESS}, so
 * whether any of its code is covered says whether a search got there.
 */
public final class ExpressDispatch {

    private static final Logger log = LoggerFactory.getLogger(ExpressDispatch.class);

    private ExpressDispatch() {
    }

    static void book(OrderPlaced order) {
        log.debug("Courier booked for order {}", order.getOrderId());
    }
}
