package com.foo.asyncapi.kafka.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Consumes {@link OrderPlaced}. Every order is confirmed and audited, through {@link OrderEvents},
 * before the listener returns. An express order is also dispatched on another thread, a while
 * after the listener has returned.
 */
@Component
public class OrderListener {

    public static final String ORDER_PLACED = "order.placed";

    public static final String LISTENER_ID = "orders";

    /**
     * Not listed in the contract, so random strings do not get there.
     */
    public static final String EXPRESS = "EXPRESS";

    /**
     * How long dispatching takes, in milliseconds, after the listener has returned.
     */
    public static final long DISPATCH_MS = 100;

    private final ExecutorService dispatchExecutor;

    private final OrderEvents events;

    public OrderListener(ExecutorService dispatchExecutor, OrderEvents events) {
        this.dispatchExecutor = dispatchExecutor;
        this.events = events;
    }

    @KafkaListener(id = LISTENER_ID, topics = ORDER_PLACED)
    public void onOrderPlaced(OrderPlaced order) {

        events.confirmed(order);
        events.audited(order);

        if (!EXPRESS.equals(order.getShipping())) {
            //nothing to dispatch
            return;
        }

        dispatchExecutor.submit(() -> {
            try {
                Thread.sleep(DISPATCH_MS);
            } catch (InterruptedException e) {
                //the service is shutting down
                Thread.currentThread().interrupt();
                return;
            }
            ExpressDispatch.book();
        });
    }
}
