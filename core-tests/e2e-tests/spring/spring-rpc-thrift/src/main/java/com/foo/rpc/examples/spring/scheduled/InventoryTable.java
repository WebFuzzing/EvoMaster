package com.foo.rpc.examples.spring.scheduled;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class InventoryTable {

    private final Map<String, InventoryRow> rows = new HashMap<>();

    public void upsert(InventoryRow row) {
        rows.put(row.orderId, row);
    }

    public InventoryRow findByOrderId(String orderId) {
        return rows.get(orderId);
    }

    public void reset() {
        rows.clear();
    }
}
