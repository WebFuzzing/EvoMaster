package org.evomaster.client.java.controller.db;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by arcuri82 on 12-Apr-19.
 */
public class ParserIssueTest {

    @Test
    public void testIssue() throws Exception{

        String sql = "SELECT x From Foo";
        Select select = (Select) CCJSqlParserUtil.parse(sql);

        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        Table table = (Table) plainSelect.getFromItem();
        assertNotNull(table);
        assertEquals("Foo", table.getName());

        Column column = (Column) ((SelectExpressionItem) plainSelect.getSelectItems().get(0)).getExpression();
        assertEquals("x", column.getColumnName());
        assertNotNull(column.getTable()); // 1.4 and 2.0 fail being "null"
        assertEquals("Foo", column.getTable().getName()); // 1.1 fails being "null"
    }
}
