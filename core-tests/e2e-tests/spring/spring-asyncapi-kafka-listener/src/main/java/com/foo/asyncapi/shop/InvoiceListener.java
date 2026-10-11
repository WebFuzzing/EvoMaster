package com.foo.asyncapi.shop;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Consumes {@link InvoiceRequested}, and leaves the rendering to another thread: the listener
 * returns at once, and the invoice is rendered a while later. Neither the listener returning nor
 * the record being committed says the service is done; only waiting long enough does.
 */
@Component
public class InvoiceListener {

    /**
     * How long rendering an invoice takes, in milliseconds. Long enough for the listener to have
     * returned, and the offset to have been committed, well before the invoice is rendered.
     */
    static final long RENDERING_MS = 150;

    private static final String VALUE_TYPE =
            JsonDeserializer.VALUE_DEFAULT_TYPE + "=com.foo.asyncapi.shop.InvoiceRequested";

    private final ExecutorService invoiceExecutor;

    public InvoiceListener(ExecutorService invoiceExecutor) {
        this.invoiceExecutor = invoiceExecutor;
    }

    @KafkaListener(id = ShopTopics.INVOICES_LISTENER, topics = ShopTopics.INVOICES_REQUESTED, properties = VALUE_TYPE)
    public void onInvoiceRequested(InvoiceRequested request) {

        invoiceExecutor.submit(() -> {
            try {
                Thread.sleep(RENDERING_MS);
            } catch (InterruptedException e) {
                //the service is shutting down
                Thread.currentThread().interrupt();
                return;
            }
            InvoiceRenderer.render(request);
        });
    }
}
