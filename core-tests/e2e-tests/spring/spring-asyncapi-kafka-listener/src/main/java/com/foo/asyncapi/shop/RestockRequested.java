package com.foo.asyncapi.shop;

/**
 * A request to order more items of a product from its supplier, as published on
 * {@link ShopTopics#RESTOCK_REQUESTED}. Every field is null when the message left it out.
 */
public class RestockRequested {

    private String sku;

    private Integer quantity;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
