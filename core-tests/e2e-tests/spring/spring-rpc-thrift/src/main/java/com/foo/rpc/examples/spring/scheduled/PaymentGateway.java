package com.foo.rpc.examples.spring.scheduled;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaymentGateway {

    private final Map<String, PaymentAuthorization> authorizations = new HashMap<>();

    public PaymentAuthorization authorize(String orderId) {
        return authorizations.get(orderId);
    }

    public void setAuthorization(PaymentAuthorization authorization) {
        authorizations.put(authorization.orderId, authorization);
    }

    public void reset() {
        authorizations.clear();
    }
}
