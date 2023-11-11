package org.evomaster.client.java.sql.distance.advanced.query_distance;

import org.evomaster.client.java.sql.distance.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.distance.advanced.driver.row.Row;
import org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance.WhereDistanceCalculator;
import org.evomaster.client.java.sql.distance.advanced.select_query.SelectQuery;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.List;

import static java.lang.String.format;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.Distance.INF_DISTANCE;
import static org.evomaster.client.java.sql.distance.advanced.query_distance.where_distance.WhereDistanceCalculator.createWhereDistanceCalculator;
import static org.evomaster.client.java.sql.distance.advanced.select_query.SelectQuery.createSelectQuery;

/**
 * Class used to calculate the distance for a query.
 */
public class QueryDistanceCalculator {

  private SelectQuery query;
  private SqlDriver sqlDriver;

  private QueryDistanceCalculator(SelectQuery query, SqlDriver sqlDriver) {
    this.query = query;
    this.sqlDriver = sqlDriver;
  }

  public static QueryDistanceCalculator createQueryDistanceCalculator(SelectQuery query, SqlDriver sqlDriver) {
    return new QueryDistanceCalculator(query, sqlDriver);
  }

  public static QueryDistanceCalculator createQueryDistanceCalculator(String queryString, SqlDriver sqlDriver) {
    return createQueryDistanceCalculator(createSelectQuery(queryString), sqlDriver);
  }

  public Distance calculate() {
    SimpleLogger.debug(format("Calculating distance for query: %s", query));
    if(query.isRestricted()) {
      SelectQuery unrestrictedQuery = query.unrestrict();
      List<Row> unrestrictedQueryRows = sqlDriver.query(unrestrictedQuery.toString());
      if(!unrestrictedQueryRows.isEmpty()) {
        return calculate(unrestrictedQueryRows);
      } else {
        return INF_DISTANCE;
      }
    } else {
      return INF_DISTANCE;
    }
  }

  private Distance calculate(List<Row> rows) {
    return rows.stream()
      .map(row -> createWhereDistanceCalculator(query, row))
      .map(WhereDistanceCalculator::calculate)
      .min(Distance::compareTo)
      .orElseThrow(() -> new AssertionError("Rows must not be empty"));
  }
}