package org.evomaster.client.java.sql.advanced.select_query;

import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.Select;
import org.evomaster.client.java.sql.advanced.helpers.SqlParserHelper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FunctionFinderTest {

    @Test
    public void testGetFunctions() { //Include F1, F2 and F3 and exclude F4
        Select select = (Select) SqlParserHelper.parseStatement("SELECT F1(a), COUNT(*) FROM t1 JOIN t2 ON F2(b) = c WHERE " +
            "d > 20 AND F3(e) = f AND EXISTS (SELECT * FROM t3 WHERE F4(g) = h) GROUP BY a");
        FunctionFinder functionFinder = new FunctionFinder(select);
        List<Function> functions = functionFinder.getFunctions();
        assertEquals(functions.size(), 3);
        assertEquals(functions.get(0).toString(), "F1(a)");
        assertEquals(functions.get(1).toString(), "F2(b)");
        assertEquals(functions.get(2).toString(), "F3(e)");
    }
}
