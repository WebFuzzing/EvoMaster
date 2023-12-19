import org.evomaster.client.java.sql.internal.constraint.DbTableCheckExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Smt2WriterTest {

    @Test
    public void productPrice() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK (price>0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(PRODUCT_PRICE, text);
    }

    @Test
    public void productPriceWithSpaces() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", " CHECK ( price > 0 ) "));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(PRODUCT_PRICE, text);
    }

    @Test
    public void productPriceWithoutSpaces() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addConstraint(new DbTableCheckExpression("products", "CHECK(price>0)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(PRODUCT_PRICE, text);
    }

    private final String PRODUCT_PRICE = "(set-logic QF_LIA)\n" +
            "(declare-const price Int)\n" +
            "(assert (> price 0))\n" +
            "(check-sat)\n" +
            "(get-value (price))\n";
}

