package com.foo.rest.emb.json.proxyprint;

import java.util.*;

public class PrintRequest {
    private long id;

    public long getId() {
        return id;
    }

    public Map<Long, String> calcBudgetsForPrintShops(List<PrintShop> pshops) {
        Map<Long, String> budgets = new HashMap<>();

        for (PrintShop printShop : pshops) {
            budgets.put(printShop.getId(), String.valueOf(Math.random() * 1001)); // add to budgets
        }

        return budgets;
    }

}
