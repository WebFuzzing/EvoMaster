package com.foo.asyncapi.shop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The topics the service consumes, as named in {@code asyncapi/shop-kafka.yaml}, and the ids of
 * the listeners that consume them. Each id is also the listener's consumer group.
 */
public final class ShopTopics {

    public static final String ORDERS_PLACED = "shop.orders.placed";

    public static final String INVOICES_REQUESTED = "shop.invoices.requested";

    public static final String STOCK_ADJUSTED = "shop.stock.adjusted";

    public static final String ORDERS_LISTENER = "shop-orders";

    public static final String INVOICES_LISTENER = "shop-invoices";

    public static final String STOCK_LISTENER = "shop-stock";

    /**
     * Every topic above, for whoever has to create them before the service starts.
     */
    public static final List<String> ALL =
            Collections.unmodifiableList(Arrays.asList(ORDERS_PLACED, INVOICES_REQUESTED, STOCK_ADJUSTED));

    private ShopTopics() {
    }
}
