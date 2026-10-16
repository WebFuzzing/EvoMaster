package com.foo.asyncapi.shop;

/**
 * A change to how many items of a product are in stock, as published on
 * {@link ShopTopics#STOCK_ADJUSTED}. Every field is null when the message left it out.
 */
public class StockAdjusted {

    private String sku;

    /**
     * How many items were added, or taken away when negative.
     */
    private Integer delta;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }
}
