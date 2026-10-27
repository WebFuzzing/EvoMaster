package com.foo.asyncapi.shop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders an invoice. Only ever run on the thread {@link InvoiceListener} hands its work to, after
 * the listener has returned, so whether any of its code is covered says whether the work done
 * after the handler was still credited to the message.
 */
public final class InvoiceRenderer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceRenderer.class);

    private InvoiceRenderer() {
    }

    static void render(InvoiceRequested request) {
        Integer cents = request.getAmountCents();
        String total = cents == null ? "?" : String.valueOf(cents);
        log.debug("Invoice for order {} rendered, total {}", request.getOrderId(), total);
    }
}
