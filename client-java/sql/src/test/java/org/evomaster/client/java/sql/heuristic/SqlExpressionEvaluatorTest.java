package org.evomaster.client.java.sql.heuristic;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.distance.heuristics.Truthness;
import org.evomaster.client.java.sql.DataRow;
import org.evomaster.client.java.sql.QueryResult;
import org.evomaster.client.java.sql.VariableDescriptor;
import org.evomaster.client.java.sql.internal.SqlNameContext;
import org.evomaster.client.java.sql.internal.SqlParserUtils;
import org.evomaster.client.java.sql.internal.TaintHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SqlExpressionEvaluatorTest {

    @Test
    public void testAndCondition() {
        String sqlCommand = "SELECT name, age FROM Persons WHERE age>18 AND age<30";
        Statement parsedSqlCommand = SqlParserUtils.parseSqlCommand(sqlCommand);
        Select select = (Select) parsedSqlCommand;

        DataRow row = new DataRow(
                "Persons",
                Arrays.asList("name", "age"),
                Arrays.asList("John", 25));

        SqlNameContext sqlNameContext = new SqlNameContext(parsedSqlCommand);
        TaintHandler taintHandler = null;

        SqlExpressionEvaluator evaluator = new SqlExpressionEvaluator(sqlNameContext, taintHandler, row);
        select.getPlainSelect().getWhere().accept(evaluator);

        Truthness truthness = evaluator.getEvaluatedTruthness();
        assertTrue(truthness.isTrue());
    }



}
