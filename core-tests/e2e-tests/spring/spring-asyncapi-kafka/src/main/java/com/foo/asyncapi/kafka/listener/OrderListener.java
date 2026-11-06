package com.foo.asyncapi.kafka.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;

/**
 * Consumes {@link OrderPlaced}. An express order is dispatched on another thread, a while after
 * the listener has returned.
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

    public OrderListener(ExecutorService dispatchExecutor) {
        this.dispatchExecutor = dispatchExecutor;
    }

    @KafkaListener(id = LISTENER_ID, topics = ORDER_PLACED)
    public void onOrderPlaced(OrderPlaced order) {

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
