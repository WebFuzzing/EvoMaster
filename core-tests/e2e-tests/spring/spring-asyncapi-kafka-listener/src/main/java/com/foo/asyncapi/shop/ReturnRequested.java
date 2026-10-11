package com.foo.asyncapi.shop;

/**
 * A customer sending back the items of an order, as published on
 * {@link ShopTopics#RETURNS_REQUESTED}. Every field is null when the message left it out.
 *
 * The document does not describe this payload at all: this class is the only place its shape is
 * written down.
 */
public class ReturnRequested {

    private String orderId;

    /**
     * Why the items are sent back. Only {@link ReturnListener#DAMAGED} means anything to the
     * service.
     */
    private String reason;

    private Integer items;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getItems() {
        return items;
    }

    public void setItems(Integer items) {
        this.items = items;
    }
}
