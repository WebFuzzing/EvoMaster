package com.foo.asyncapi.shop;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * How many items of each product are in stock, kept in the {@code stock} table of an in-memory
 * database.
 *
 * The first adjustment of a product records it as it comes; the ones after it add to what is
 * there. Selling more than what is in stock can therefore only happen on a product an earlier
 * message already recorded, which takes two messages, in order, in the same test.
 */
@Component
public class StockLedger {

    static final String TABLE = "stock";

    private static final String SELECT = "SELECT quantity FROM " + TABLE + " WHERE sku = ?";

    private static final String INSERT = "INSERT INTO " + TABLE + " (sku, quantity) VALUES (?, ?)";

    private static final String UPDATE = "UPDATE " + TABLE + " SET quantity = ? WHERE sku = ?";

    private final JdbcTemplate jdbc;

    public StockLedger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void adjust(String sku, int delta) {

        List<Integer> current = jdbc.queryForList(SELECT, Integer.class, sku);

        if (current.isEmpty()) {
            jdbc.update(INSERT, sku, delta);
            return;
        }

        int quantity = current.get(0) + delta;
        jdbc.update(UPDATE, quantity, sku);

        if (quantity < 0) {
            OversoldStock.record(sku, quantity);
        }
    }
}
