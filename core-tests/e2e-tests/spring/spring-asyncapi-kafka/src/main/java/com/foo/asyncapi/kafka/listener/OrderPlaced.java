package com.foo.asyncapi.kafka.listener;

/**
 * A new order, as published on {@link OrderListener#ORDER_PLACED}.
 */
public class OrderPlaced {

    /**
     * Free text in the contract; only {@link OrderListener#EXPRESS} means anything to the service.
     */
    private String shipping;

    public String getShipping() {
        return shipping;
    }

    public void setShipping(String shipping) {
        this.shipping = shipping;
    }
}
