package com.foo.asyncapi.kafka.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reached only for an express order, so whether its code is covered says whether a search got there.
 */
public final class ExpressDispatch {

    private static final Logger log = LoggerFactory.getLogger(ExpressDispatch.class);

    private ExpressDispatch() {
    }

    static void book() {
        log.debug("Express order dispatched");
    }
}
