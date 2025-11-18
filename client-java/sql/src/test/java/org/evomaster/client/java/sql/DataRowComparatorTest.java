package org.evomaster.client.java.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.OrderByElement;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataRowComparatorTest {

    private static final List<VariableDescriptor> DESCRIPTORS = Arrays.asList(
            new VariableDescriptor("name", "name", "users"),
            new VariableDescriptor("age", "age", "users")
    );

    private static final DataRow ROW_ALICE_25 = new DataRow(DESCRIPTORS, Arrays.asList("Alice", 25));
    private static final DataRow ROW_BOB_30 = new DataRow(DESCRIPTORS, Arrays.asList("Bob", 30));
    private static final DataRow ROW_ALICE_30 = new DataRow(DESCRIPTORS, Arrays.asList("Alice", 30));

    private OrderByElement createOrderBy(String columnName, boolean asc) {
        OrderByElement orderByElement = new OrderByElement();
        try {
            Expression expression = CCJSqlParserUtil.parseExpression(columnName);
            orderByElement.setExpression(expression);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        orderByElement.setAsc(asc);
        return orderByElement;
    }

    @Test
    void testSingleColumnAscending() {
        DataRowComparator comparator = new DataRowComparator(Collections.singletonList(createOrderBy("name", true)));
        assertTrue(comparator.compare(ROW_ALICE_25, ROW_BOB_30) < 0);
        assertEquals(0, comparator.compare(ROW_ALICE_25, ROW_ALICE_30));
    }

    @Test
    void testSingleColumnDescending() {
        DataRowComparator comparator = new DataRowComparator(Collections.singletonList(createOrderBy("name", false)));
        assertTrue(comparator.compare(ROW_ALICE_25, ROW_BOB_30) > 0);
        assertEquals(0, comparator.compare(ROW_ALICE_25, ROW_ALICE_30));
    }

    @Test
    void testMultipleColumns() {
        List<OrderByElement> orderByElements = Arrays.asList(
                createOrderBy("name", true),
                createOrderBy("age", true)
        );
        DataRowComparator comparator = new DataRowComparator(orderByElements);
        assertTrue(comparator.compare(ROW_ALICE_25, ROW_ALICE_30) < 0);
        assertTrue(comparator.compare(ROW_ALICE_30, ROW_ALICE_25) > 0);
    }

    @Test
    void testNullValues() {
        DataRow rowWithNull = new DataRow(DESCRIPTORS, Arrays.asList(null, 25));
        DataRowComparator comparator = new DataRowComparator(Collections.singletonList(createOrderBy("name", true)));
        assertTrue(comparator.compare(rowWithNull, ROW_ALICE_25) < 0);
    }
}
