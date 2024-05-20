package org.evomaster.client.java.sql.advanced.query_calculator;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator;
import org.evomaster.client.java.sql.advanced.select_query.SelectQuery;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.advanced.query_calculator.CalculationResult.createCalculationResult;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator.createWhereCalculator;
import static org.evomaster.client.java.sql.advanced.select_query.SelectQuery.createSelectQuery;

public class QueryCalculator {

  private SelectQuery query;
  private SqlDriver sqlDriver;

  private QueryCalculator(SelectQuery query, SqlDriver sqlDriver) {
    this.query = query;
    this.sqlDriver = sqlDriver;
  }

  public static QueryCalculator createQueryCalculator(SelectQuery query, SqlDriver sqlDriver) {
    return new QueryCalculator(query, sqlDriver);
  }

  public static QueryCalculator createQueryCalculator(String queryString, SqlDriver sqlDriver) {
    return new QueryCalculator(createSelectQuery(queryString), sqlDriver);
  }

  public CalculationResult calculate() {
    SimpleLogger.debug(format("Calculating truthness for query: %s", query));
    if(query.isPlainSelect()){
      if(query.hasWhere()) {
        Truthness rowSetTruthness = calculateRowSet().getTruthness();
        Truthness conditionTruthness = calculateCondition().getTruthness();
        return createCalculationResult(andAggregation(rowSetTruthness, conditionTruthness));
      } else {
        return calculateRowSet();
      }
    } else if(query.isUnion()) {
      return calculateUnion();
    } else {
      throw new UnsupportedOperationException(format("Unsupported query: %s", query.toString()));
    }
  }

  private CalculationResult calculateCondition() {
    List<Row> queryRows = sqlDriver.query(query);
    if(queryRows.isEmpty()) {
      SelectQuery queryWithoutWhere = query.removeWhere();
      List<Row> queryWithoutWhereRows = sqlDriver.query(queryWithoutWhere);
      if(!queryWithoutWhereRows.isEmpty()) {
        Double maxRowTruthness = queryWithoutWhereRows.stream()
          .map(row -> createWhereCalculator(query, row, sqlDriver))
          .map(WhereCalculator::calculate)
          .map(Truthness::getOfTrue)
          .max(Double::compareTo)
          .orElseThrow(() -> new RuntimeException("Rows must not be empty"));
        return createCalculationResult(scaleTrue(maxRowTruthness));
      } else {
        return createCalculationResult(FALSE);
      }
    } else {
      return createCalculationResult(queryRows);
    }
  }

  private CalculationResult calculateRowSet() {
    if(query.hasFromSubquery()) {
      return calculateFromSubquery();
    } else {
      if(query.hasJoins()){
        return calculateJoins();
      } else {
        if(query.hasFrom()) {
          return calculateSingleTable();
        } else {
          return calculateNoTable();
        }
      }
    }
  }

  private CalculationResult calculateFromSubquery() {
    QueryCalculator queryCalculator = createQueryCalculator(query.getFromSubquery(), sqlDriver);
    return queryCalculator.calculate();
  }

  private CalculationResult calculateJoins() {
    if(query.getJoinSelects().size() == 2){
      if(query.isInnerJoin()){
        return applyOnQueries(
          Stream.concat(query.getJoinSelects().stream(), Stream.of(query.convertToCrossJoin())).collect(Collectors.toList()),
          truthnesses -> createCalculationResult(andAggregation(truthnesses)));
      } else if(query.isFullJoin() || query.isCrossJoin()){
        return applyOnQueries(query.getJoinSelects(),
          truthnesses -> createCalculationResult(query.isFullJoin() ? orAggregation(truthnesses) : andAggregation(truthnesses)));
      } else if(query.isLeftJoin() || query.isRightJoin()){
        return applyOnQueries(
          singletonList(query.isLeftJoin() ? query.getJoinSelects().get(0) : query.getJoinSelects().get(1)),
          truthnesses -> createCalculationResult(truthnesses[0]));
      } else {
        throw new UnsupportedOperationException(format("Unsupported JOIN type in query: %s", query.toString()));
      }
    } else {
      throw new UnsupportedOperationException(format("Unsupported JOIN in query: %s", query.toString()));
    }
  }

  private CalculationResult applyOnQueries(List<SelectQuery> queries, Function<Truthness[], CalculationResult> function){
    return function.apply(calculateQueries(queries).stream()
      .map(CalculationResult::getTruthness)
      .toArray(Truthness[]::new));
  }

  private List<CalculationResult> calculateQueries(List<SelectQuery> queries){
    return queries.stream()
      .map(query -> createQueryCalculator(query, sqlDriver))
      .map(QueryCalculator::calculate)
      .collect(Collectors.toList());
  }

  private CalculationResult calculateSingleTable() {
    SelectQuery queryWithoutWhere = query.removeWhere();
    List<Row> queryWithoutWhereRows = sqlDriver.query(queryWithoutWhere);
    Truthness truthness = getTruthnessToEmpty(queryWithoutWhereRows.size()).invert();
    return createCalculationResult(truthness);
  }

  private CalculationResult calculateNoTable() {
    List<Row> rows = sqlDriver.query(query);
    return createCalculationResult(rows);
  }

  private CalculationResult calculateUnion() {
    List<CalculationResult> calculationResults = calculateQueries(query.getSetOperationSelects());
    return createCalculationResult(
      orAggregation(calculationResults.stream()
        .map(CalculationResult::getTruthness)
        .toArray(Truthness[]::new)),
      calculationResults.stream()
        .map(CalculationResult::getRows)
        .flatMap(List::stream)
        .collect(Collectors.toList()));
  }
}