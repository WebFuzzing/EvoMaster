namespace java com.foo.rpc.examples.spring.scheduled

struct PaymentAuthorization {
    1: string orderId,
    2: bool approved
}

struct InventoryRow {
    1: string orderId,
    2: i32 stock,
    3: bool priorityCustomer
}

struct InventoryRefreshTask {
    1: string taskName
}

service OrderFulfillmentService {

    string evaluateOrder(1: string orderId),

    bool backdoor(1: PaymentAuthorization payment, 2: InventoryRow inventory, 3: InventoryRefreshTask refreshTask)
}
