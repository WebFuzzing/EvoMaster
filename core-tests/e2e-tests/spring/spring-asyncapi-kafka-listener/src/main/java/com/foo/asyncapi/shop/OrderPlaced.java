package com.foo.asyncapi.shop;

/**
 * A new order, as published on {@link ShopTopics#ORDERS_PLACED}. Every field is null when the
 * message left it out.
 */
public class OrderPlaced {

    private String orderId;

    /**
     * How the order is to be shipped. Free text in the contract; only {@code EXPRESS} means
     * anything to the service.
     */
    private String shipping;

    /**
     * The name of a {@link Priority}, or anything else, which the service ignores.
     */
    private String priority;

    private Integer quantity;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getShipping() {
        return shipping;
    }

    public void setShipping(String shipping) {
        this.shipping = shipping;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
