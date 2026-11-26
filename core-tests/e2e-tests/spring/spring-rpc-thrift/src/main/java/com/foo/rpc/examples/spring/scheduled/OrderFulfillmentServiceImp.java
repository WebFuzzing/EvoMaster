package com.foo.rpc.examples.spring.scheduled;

import org.apache.thrift.TException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderFulfillmentServiceImp implements OrderFulfillmentService.Iface {

    private final InventorySnapshotCache inventoryCache;
    private final InventoryTable inventoryTable;
    private final PaymentGateway paymentGateway;

    public OrderFulfillmentServiceImp(InventorySnapshotCache inventoryCache,
                                      InventoryTable inventoryTable,
                                      PaymentGateway paymentGateway) {
        this.inventoryCache = inventoryCache;
        this.inventoryTable = inventoryTable;
        this.paymentGateway = paymentGateway;
    }

    /*
     * In production this hourly task refreshes the inventory snapshot consumed
     * by evaluateOrder. Without this task, requests must wait for a fresh view.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void refreshInventorySnapshot() {
        inventoryCache.markRefreshed();
    }

    @Override
    public String evaluateOrder(String orderId) throws TException {
        if (inventoryCache.getRefreshCount() == 0) {
            return "PENDING_INVENTORY_REFRESH";
        }

        InventoryRow row = inventoryTable.findByOrderId(orderId);
        if (row == null) {
            return "UNKNOWN_ORDER";
        }
        if (row.stock <= 0) {
            return "OUT_OF_STOCK";
        }

        PaymentAuthorization authorization = paymentGateway.authorize(orderId);
        if (authorization == null || !authorization.approved) {
            return "PAYMENT_DECLINED";
        }

        if (row.priorityCustomer) {
            return "APPROVED_PRIORITY";
        }

        return "APPROVED_STANDARD";
    }

    @Override
    public boolean backdoor(PaymentAuthorization payment, InventoryRow inventory, InventoryRefreshTask refreshTask) throws TException {
        if (payment == null && inventory == null && refreshTask == null) {
            inventoryCache.reset();
            inventoryTable.reset();
            paymentGateway.reset();
            return true;
        }

        if (payment != null) {
            paymentGateway.setAuthorization(payment);
        }
        if (inventory != null) {
            inventoryTable.upsert(inventory);
        }
        if (refreshTask != null && "refreshInventorySnapshot".equals(refreshTask.taskName)) {
            refreshInventorySnapshot();
        }

        return true;
    }
}
