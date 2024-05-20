package org.evomaster.client.java.sql.advanced.query_contextualizer;

import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.sql.advanced.driver.Schema;
import org.evomaster.client.java.sql.advanced.driver.SqlDriver;
import org.evomaster.client.java.sql.advanced.driver.row.Row;
import org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext;
import org.evomaster.client.java.sql.advanced.helpers.SqlParserHelper;
import org.evomaster.client.java.sql.advanced.select_query.QueryTable;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.evomaster.client.java.sql.advanced.CollectionsHelper.createList;
import static org.evomaster.client.java.sql.advanced.CollectionsHelper.createMap;
import static org.evomaster.client.java.sql.advanced.ObjectMother.createRow;
import static org.evomaster.client.java.sql.advanced.ObjectMother.createSimpleEvaluationContext;
import static org.evomaster.client.java.sql.advanced.evaluation_context.EvaluationContext.createEvaluationContext;
import static org.evomaster.client.java.sql.advanced.query_contextualizer.SubQueryContextualizer.createSubQueryContextualizer;
import static org.evomaster.client.java.sql.advanced.select_query.QueryTable.createQueryTable;

public class SubQueryContextualizerTest {

    @Test
    public void testContextualize() { //Single level contextualization
        List<QueryTable> tables = singletonList(createQueryTable("t0"));
        Row row = createRow("t0", createMap("a", 10, "b", "string"));
        EvaluationContext evaluationContext = createEvaluationContext(tables, row);

        List<String> t1 = createList("c", "d");
        List<String> t2 = createList("e", "f");
        Schema schema = new Schema(createMap("t1", t1, "t2", t2));

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);
        Mockito.when(sqlDriverMock.getSchema()).thenReturn(schema);

        Select select = (Select) SqlParserHelper.parseStatement("SELECT a FROM t1 JOIN t2 ON a = 1 WHERE a = 2 AND c = b AND d = UPPER(b)");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("SELECT 10 FROM t1 JOIN t2 ON 10 = 1 WHERE 10 = 2 AND c = 'string' AND d = UPPER('string')", sql);
    }

    @Test
    public void testContextualize2() { //Multi level contextualization
        EvaluationContext evaluationContext = createSimpleEvaluationContext("t0", "a", 10);

        List<String> t1 = singletonList("b");
        List<String> t2 = singletonList("a");
        List<String> t3 = singletonList("c");
        Schema schema = new Schema(createMap("t1", t1, "t2", t2, "t3", t3));

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);
        Mockito.when(sqlDriverMock.getSchema()).thenReturn(schema);

        Select select = (Select) SqlParserHelper.parseStatement("SELECT * FROM t1 WHERE " +
            "a = 1 AND EXISTS (SELECT * FROM t2 WHERE " +
            "a = 2 AND EXISTS (SELECT * FROM t3 WHERE " +
            "a = c))");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("SELECT * FROM t1 WHERE " +
            "10 = 1 AND EXISTS (SELECT * FROM t2 WHERE " +
            "a = 2 AND EXISTS (SELECT * FROM t3 WHERE " +
            "a = c))", sql);
    }

    @Test
    public void testContextualize3() { //Multi table contextualization
        EvaluationContext evaluationContext = createSimpleEvaluationContext("t0", "a", 10);

        List<String> t1 = singletonList("b");
        List<String> t2 = singletonList("a");
        List<String> t3 = singletonList("d");
        Schema schema = new Schema(createMap("t1", t1, "t2", t2, "t3", t3));

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);
        Mockito.when(sqlDriverMock.getSchema()).thenReturn(schema);

        Select select = (Select) SqlParserHelper.parseStatement("SELECT * FROM t1 JOIN t2 WHERE " +
            "a = b AND EXISTS (SELECT * FROM t3 WHERE a = d)");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("SELECT * FROM t1 JOIN t2 WHERE " +
            "a = b AND EXISTS (SELECT * FROM t3 WHERE a = d)", sql);
    }

    @Test
    public void testContextualize4() { //Multi level contextualization with multi table contextualization
        EvaluationContext evaluationContext = createSimpleEvaluationContext("t0", "a", 10);

        List<String> t1 = singletonList("b");
        List<String> t2 = singletonList("c");
        List<String> t3 = singletonList("a");
        Schema schema = new Schema(createMap("t1", t1, "t2", t2, "t3", t3));

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);
        Mockito.when(sqlDriverMock.getSchema()).thenReturn(schema);

        Select select = (Select) SqlParserHelper.parseStatement("SELECT * FROM t1 WHERE " +
            "a = b AND EXISTS (SELECT * FROM t2 JOIN t3 WHERE a = b)");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("SELECT * FROM t1 WHERE " +
            "10 = b AND EXISTS (SELECT * FROM t2 JOIN t3 WHERE a = b)", sql);
    }

    @Test
    public void testContextualize5() { //Union
        EvaluationContext evaluationContext = createSimpleEvaluationContext("t0", "a", 10);

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);

        Select select = (Select) SqlParserHelper.parseStatement("(SELECT * FROM t1 WHERE " +
            "a = 10) UNION ALL (SELECT * FROM t1 WHERE a = 10)");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("(SELECT * FROM t1 WHERE " +
            "10 = 10) UNION ALL (SELECT * FROM t1 WHERE 10 = 10)", sql);
    }

    @Test
    public void testContextualize6() { //Subquery in JOIN
        EvaluationContext evaluationContext = createSimpleEvaluationContext("t0", "a", 10);

        List<String> t1 = singletonList("b");
        List<String> t2 = singletonList("c");
        Schema schema = new Schema(createMap("t1", t1, "t2", t2));

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);
        Mockito.when(sqlDriverMock.getSchema()).thenReturn(schema);

        Select select = (Select) SqlParserHelper.parseStatement("SELECT * FROM t1 JOIN (SELECT * FROM t2 WHERE a = 10)");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("SELECT * FROM t1 JOIN (SELECT * FROM t2 WHERE 10 = 10)", sql);
    }

    @Test
    public void testContextualize7() { //Subquery in FROM
        EvaluationContext evaluationContext = createSimpleEvaluationContext("t0", "a", 10);

        List<String> t1 = singletonList("b");
        Schema schema = new Schema(createMap("t1", t1));

        SqlDriver sqlDriverMock = Mockito.mock(SqlDriver.class);
        Mockito.when(sqlDriverMock.getSchema()).thenReturn(schema);

        Select select = (Select) SqlParserHelper.parseStatement("SELECT * FROM (SELECT * FROM t1 WHERE a = 10)");
        SubQueryContextualizer subQueryContextualizer = createSubQueryContextualizer(select, evaluationContext, sqlDriverMock);
        String sql = subQueryContextualizer.contextualize();

        Assert.assertEquals("SELECT * FROM (SELECT * FROM t1 WHERE 10 = 10)", sql);
    }
}
