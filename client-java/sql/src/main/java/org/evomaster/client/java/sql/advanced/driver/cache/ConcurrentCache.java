package org.evomaster.client.java.sql.advanced.driver.cache;

import org.evomaster.client.java.sql.advanced.driver.row.Row;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ConcurrentCache implements Cache {

    private ConcurrentMap<String, List<Row>> map;

    public ConcurrentCache() {
        map = new ConcurrentHashMap<>();
    }

    @Override
    public Boolean isCached(String sql) {
        return map.containsKey(sql);
    }

    @Override
    public void cache(String sql, List<Row> result) {
        map.put(sql, result);
    }

    @Override
    public List<Row> get(String sql) {
        return map.get(sql);
    }

    @Override
    public String toString() {
        return map.entrySet()
            .stream()
            .map(e -> e.getKey() + " = " + e.getValue())
            .collect(Collectors.joining("\n"));
    }
}
