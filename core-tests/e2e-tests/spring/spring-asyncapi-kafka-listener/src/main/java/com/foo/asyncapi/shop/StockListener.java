package com.foo.asyncapi.shop;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link StockAdjusted}, keeps the stock of each product in the database through
 * {@link StockLedger}, and announces the change through {@link ShopEvents}.
 */
@Component
public class StockListener {

    private static final String VALUE_TYPE =
            JsonDeserializer.VALUE_DEFAULT_TYPE + "=com.foo.asyncapi.shop.StockAdjusted";

    private final StockLedger ledger;

    private final ShopEvents events;

    public StockListener(StockLedger ledger, ShopEvents events) {
        this.ledger = ledger;
        this.events = events;
    }

    @KafkaListener(id = ShopTopics.STOCK_LISTENER, topics = ShopTopics.STOCK_ADJUSTED, properties = VALUE_TYPE)
    public void onStockAdjusted(StockAdjusted adjustment) {

        if (adjustment.getSku() == null || adjustment.getDelta() == null) {
            //nothing to adjust
            return;
        }
        ledger.adjust(adjustment.getSku(), adjustment.getDelta());
        events.stockChanged(adjustment.getSku(), adjustment.getDelta());
    }
}
