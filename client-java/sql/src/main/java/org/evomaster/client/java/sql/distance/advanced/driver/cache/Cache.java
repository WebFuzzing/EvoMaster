package org.evomaster.client.java.sql.distance.advanced.driver.cache;

import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;

import java.util.List;

public interface Cache {

    Boolean isCached(String sql);

    void cache(String sql, List<Row> result);

    List<Row> get(String sql);

    String dump();
}
