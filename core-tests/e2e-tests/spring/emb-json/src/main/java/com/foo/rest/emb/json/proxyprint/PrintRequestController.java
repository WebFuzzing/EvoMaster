package com.foo.rest.emb.json.proxyprint;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.*;

/**
 * This code is taken from proxyprint-kitchen
 * G: https://github.com/ProxyPrint/proxyprint-kitchen
 * L: Apache 2.0
 * P: src/main/java/io/github/proxyprint/kitchen/controllers/consumer/PrintRequestController.java
 */
public class PrintRequestController {

    private Map<Long, PrintShop> printShops;

    // This is not part of the original class, added for testing purposes
    PrintRequestController() {
        this.printShops = new HashMap<>();
        this.printShops.put(3L, new PrintShop(3L));
        this.printShops.put(12L, new PrintShop(12L));
    }

    public Map<Long, String> calcBudgetForPrintRequest(String requestJSON) throws IOException {
        PrintRequest printRequest = new PrintRequest();

        List<Long> pshopIDs = null;
        Map prequest = new Gson().fromJson(requestJSON, Map.class);

        // PrintShops
        List<Double> tmpPshopIDs = (List<Double>) prequest.get("printshops");
        pshopIDs = new ArrayList<>();
        for (double doubleID : tmpPshopIDs) {
            pshopIDs.add((long) Double.valueOf((double) doubleID).intValue());
        }

        // Finally, calculate the budgets :D
        List<PrintShop> pshops = getListOfPrintShops(pshopIDs);
        Map<Long, String> budgets = printRequest.calcBudgetsForPrintShops(pshops);

        return budgets;
    }

    public List<PrintShop> getListOfPrintShops(List<Long> pshopsIDs) {
        List<PrintShop> pshops = new ArrayList<>();
        for (long pid : pshopsIDs) {
            pshops.add(printShops.get(pid));
        }
        return pshops;
    }

}
