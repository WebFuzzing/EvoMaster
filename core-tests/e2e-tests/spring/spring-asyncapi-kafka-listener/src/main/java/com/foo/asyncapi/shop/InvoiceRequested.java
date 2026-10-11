package com.foo.asyncapi.shop;

/**
 * A request to render the invoice of an order, as published on
 * {@link ShopTopics#INVOICES_REQUESTED}. Every field is null when the message left it out.
 */
public class InvoiceRequested {

    private String orderId;

    private Integer amountCents;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }
}
