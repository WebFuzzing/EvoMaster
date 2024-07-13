package org.evomaster.client.java.sql.advanced.query_calculator;

import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.distance.heuristics.TruthnessUtils;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator;
import org.evomaster.client.java.sql.advanced.select_query.SelectQuery;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.evomaster.client.java.distance.heuristics.Truthness.FALSE;
import static org.evomaster.client.java.distance.heuristics.Truthness.TRUE;
import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.*;
import static org.evomaster.client.java.sql.advanced.query_calculator.CalculationResult.*;
import static org.evomaster.client.java.sql.advanced.query_calculator.where_calculator.WhereCalculator.createWhereCalculator;
import static org.evomaster.client.java.sql.advanced.select_query.SelectQuery.createSelectQuery;

public class QueryCalculator {

  private SelectQuery query;
  private SqlDriver sqlDriver;
  private TaintHandler taintHandler;

  private QueryCalculator(SelectQuery query, SqlDriver sqlDriver, TaintHandler taintHandler) {
    this.query = query;
    this.sqlDriver = sqlDriver;
    this.taintHandler = taintHandler;
  }

  public static QueryCalculator createQueryCalculator(SelectQuery query, SqlDriver sqlDriver, TaintHandler taintHandler) {
    return new QueryCalculator(query, sqlDriver, taintHandler);
  }

  public static QueryCalculator createQueryCalculator(String queryString, SqlDriver sqlDriver, TaintHandler taintHandler) {
    return createQueryCalculator(createSelectQuery(queryString), sqlDriver, taintHandler);
  }

  public CalculationResult calculate() {
    SimpleLogger.debug(format("Calculating truthness for query: %s", query));
    List<Row> queryRows = sqlDriver.query(query.toString());
    if(queryRows.isEmpty()){
      return createCalculationResult(calculateNoResultsQuery(), emptyList());
    } else {
      return createCalculationResult(TRUE, queryRows);
    }
  }

  private Truthness calculateNoResultsQuery() {
      if(query.isPlainSelect()){
        if(query.hasWhere()) {
          return andAggregation(calculateRowSet(), calculateCondition());
        } else {
          return calculateRowSet();
        }
      } else if(query.isUnion()) {
        return calculateUnion();
      } else {
        throw new UnsupportedOperationException(format("Unsupported query: %s", query.toString()));
      }
  }

  private Truthness calculateCondition() {
      SelectQuery queryWithoutWhere = query.removeWhere();
      List<Row> queryWithoutWhereRows = sqlDriver.query(queryWithoutWhere.toString());
      if(!queryWithoutWhereRows.isEmpty()) {
        Double maxRowTruthness = queryWithoutWhereRows.stream()
          .map(row -> createWhereCalculator(query, row, sqlDriver, taintHandler))
          .map(WhereCalculator::calculate)
          .map(Truthness::getOfTrue)
          .max(Double::compareTo)
          .orElseThrow(() -> new RuntimeException("Rows must not be empty"));
        return scaleTrue(maxRowTruthness);
      } else {
        return FALSE;
      }
  }

  private Truthness calculateRowSet() {
    if(query.hasFromSubquery()) {
      return calculateFromSubquery();
    } else if(query.hasJoins()){
        return calculateJoins();
    } else {
        return calculateGeneric();
    }
  }

  private Truthness calculateFromSubquery() {
    QueryCalculator queryCalculator = createQueryCalculator(query.getFromSubquery(), sqlDriver, taintHandler);
    CalculationResult calculationResult = queryCalculator.calculate();
    return calculationResult.getTruthness();
  }

  private Truthness calculateJoins() {
    if(query.getJoinSelects().size() == 2){
      if(query.isInnerJoin()){
        List<SelectQuery> selects = Stream.concat(query.getJoinSelects().stream(),
          Stream.of(query.convertToCrossJoin())).collect(Collectors.toList());
        return applyOnQueries(selects, TruthnessUtils::andAggregation);
      } else if(query.isFullJoin()){
        return applyOnQueries(query.getJoinSelects(), TruthnessUtils::orAggregation);
      } else if(query.isCrossJoin()){
        return applyOnQueries(query.getJoinSelects(), TruthnessUtils::andAggregation);
      } else if(query.isLeftJoin()){
        return applyOnQueries(singletonList(query.getJoinSelects().get(0)), truthnesses -> truthnesses[0]);
      } else if(query.isRightJoin()){
        return applyOnQueries(singletonList(query.getJoinSelects().get(1)), truthnesses -> truthnesses[0]);
      }
    }
    return calculateGeneric();
  }

  private Truthness applyOnQueries(List<SelectQuery> queries, Function<Truthness[], Truthness> function){
    return function.apply(mergeTruthness(calculateQueries(queries)));
  }

  private Truthness[] mergeTruthness(List<CalculationResult> calculationResults) {
    return calculationResults.stream()
      .map(CalculationResult::getTruthness)
      .toArray(Truthness[]::new);
  }

  private List<CalculationResult> calculateQueries(List<SelectQuery> queries){
    return queries.stream()
      .map(query -> createQueryCalculator(query, sqlDriver, taintHandler))
      .map(QueryCalculator::calculate)
      .collect(Collectors.toList());
  }

  private Truthness calculateGeneric() {
    SelectQuery queryWithoutWhere = query.removeWhere();
    List<Row> queryWithoutWhereRows = sqlDriver.query(queryWithoutWhere.toString());
    return getTruthnessToEmpty(queryWithoutWhereRows.size()).invert();
  }

  private Truthness calculateUnion() {
    List<CalculationResult> calculationResults = calculateQueries(query.getSetOperationSelects());
    return orAggregation(mergeTruthness(calculationResults));
  }
}