import org.evomaster.client.java.sql.internal.constraint.DbTableCheckExpression;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Smt2WriterTest {

    // ************************************** //
    // ********** CHECK constraint ********** //
    // ************************************** //
    @Test
    public void productGreaterPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price>0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2(">", "0"), text);
    }

    @Test
    public void productGreaterPriceWithSpaces() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", " CHECK ( price > 0 ) "));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2(">", "0"), text);
    }

    @Test
    public void productGreaterPriceWithoutSpaces() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK(price>0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2(">", "0"), text);
    }

    @Test
    public void productGreaterOrEqualToPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price>=0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2(">=", "0"), text);
    }

    @Test
    public void productLowerPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price<0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2("<", "0"), text);
    }
    @Test
    public void productLowerOrEqualToPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price<=0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2("<=", "0"), text);
    }

    @Test
    public void productEqualPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price=0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2("=", "0"), text);
    }
    @Test
    public void productGreaterThanHighPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price>1000)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2(">", "1000"), text);
    }

    // TODO: Add support for multiple check constraints
    @Test
    @Disabled
    public void productGreaterPriceAndStock() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price>1000 AND stock>5)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_LIA)\n" +
                "(declare-const price Int)\n" +
                "(declare-const stock Int)\n" +
                "(assert (and (> price 1000) (> stock 5)))\n" +
                "(check-sat)\n" +
                "(get-value (price stock))\n";

        assertEquals(expected, text);
    }

    private String expectedSmt2(String cmp, String val) {
        return "(set-logic QF_LIA)\n" +
                "(declare-const price Int)\n" +
                "(assert (" + cmp + " price " + val + "))\n" +
                "(check-sat)\n" +
                "(get-value (price))\n";
    }

}

