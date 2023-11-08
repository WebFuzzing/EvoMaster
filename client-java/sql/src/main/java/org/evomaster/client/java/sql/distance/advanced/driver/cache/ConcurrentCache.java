package org.evomaster.client.java.sql.distance.advanced.driver.cache;

import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentCache implements Cache {

    private ConcurrentMap<String, List<Row>> cache;

    public ConcurrentCache() {
        cache = new ConcurrentHashMap<>();
    }

    @Override
    public Boolean isCached(String sql) {
        return cache.containsKey(sql);
    }

    @Override
    public void cache(String sql, List<Row> result) {
        cache.put(sql, result);
    }

    @Override
    public List<Row> get(String sql) {
        return cache.get(sql);
    }
}
