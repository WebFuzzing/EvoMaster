package com.foo.asyncapi.shop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The topics the service consumes and publishes to, as named in {@code asyncapi/shop-kafka.yaml}
 * except for {@link #STOCK_CHANGES}, and the ids of the listeners that consume them. Each id is
 * also the listener's consumer group.
 */
public final class ShopTopics {

    public static final String ORDERS_PLACED = "shop.orders.placed";

    public static final String INVOICES_REQUESTED = "shop.invoices.requested";

    public static final String STOCK_ADJUSTED = "shop.stock.adjusted";

    public static final String RETURNS_REQUESTED = "shop.returns.requested";

    /**
     * Where the service confirms every order it takes, as the document declares.
     */
    public static final String ORDERS_CONFIRMED = "shop.orders.confirmed";

    /**
     * Where the service announces every change of stock. Not in the document, deliberately: see
     * {@link ShopEvents}.
     */
    public static final String STOCK_CHANGES = "shop.stock.changes";

    public static final String RESTOCK_REQUESTED = "shop.restock.requested";

    public static final String ORDERS_LISTENER = "shop-orders";

    public static final String INVOICES_LISTENER = "shop-invoices";

    public static final String STOCK_LISTENER = "shop-stock";

    public static final String RETURNS_LISTENER = "shop-returns";

    public static final String RESTOCK_LISTENER = "shop-restock";

    /**
     * Every topic above, for whoever has to create them before the service starts.
     */
    public static final List<String> ALL = Collections.unmodifiableList(Arrays.asList(
            ORDERS_PLACED, INVOICES_REQUESTED, STOCK_ADJUSTED, RETURNS_REQUESTED, ORDERS_CONFIRMED, STOCK_CHANGES,
            RESTOCK_REQUESTED));

    private ShopTopics() {
    }
}
