package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pages whoever is on call about an urgent order. Only ever reached from
 * {@link OrderListener#onOrderPlaced}, for {@link Priority#URGENT}, so whether any of its code is
 * covered says whether a search got there.
 */
public final class UrgentEscalation {

    private static final Logger log = LoggerFactory.getLogger(UrgentEscalation.class);

    private UrgentEscalation() {
    }

    static void escalate(OrderPlaced order) {
        log.debug("On-call paged for order {}", order.getOrderId());
    }
}
