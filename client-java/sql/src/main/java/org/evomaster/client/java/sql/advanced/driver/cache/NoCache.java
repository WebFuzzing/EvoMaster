package org.evomaster.client.java.sql.advanced.driver.cache;

import org.evomaster.client.java.sql.advanced.driver.row.Row;

import java.util.List;

public class NoCache implements Cache {

    public NoCache() {}

    @Override
    public Boolean isCached(String sql) {
        return false;
    }

    @Override
    public void cache(String sql, List<Row> result) {

    }

    @Override
    public List<Row> get(String sql) {
        return null;
    }

    @Override
    public String toString() {
        return "empty";
    }
}
