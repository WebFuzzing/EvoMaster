package org.evomaster.client.java.sql.distance.advanced.driver.cache;

import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;

import java.util.List;

/**
 * Interface defining a cache for the returned rows of a query
 */
public interface Cache {

    Boolean isCached(String sql);

    void cache(String sql, List<Row> result);

    List<Row> get(String sql);

    String dump();
}
