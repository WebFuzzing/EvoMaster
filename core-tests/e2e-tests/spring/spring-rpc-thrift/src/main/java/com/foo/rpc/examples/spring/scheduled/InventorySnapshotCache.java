package com.foo.rpc.examples.spring.scheduled;

import org.springframework.stereotype.Service;

@Service
public class InventorySnapshotCache {

    private int refreshCount;

    public int getRefreshCount() {
        return refreshCount;
    }

    public void markRefreshed() {
        refreshCount++;
    }

    public void reset() {
        refreshCount = 0;
    }
}
