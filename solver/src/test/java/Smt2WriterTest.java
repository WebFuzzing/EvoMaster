import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.TableCheckExpressionDto;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Smt2WriterTest {

    // ************************************** //
    // ********** CHECK constraint ********** //
    // ************************************** //
    @Test
    public void productGreaterPriceAsParsed() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression("products", CheckExpressionFrom("(\"PRICE\" > 100)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-fun products_PRICE () Int)\n" +
                "(assert (> products_PRICE 100))\n" +
                "(check-sat)\n" +
                "(get-value (products_PRICE))\n";

        assertEquals(expected, text);
    }

    @Test
    public void productGreaterAndLowerPriceAsParsed() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression("products", CheckExpressionFrom("(\"PRICE\" > 100 AND \"PRICE\" < 9999)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-fun products_PRICE () Int)\n" +
                "(assert (and (> products_PRICE 100) (< products_PRICE 9999)))\n" +
                "(check-sat)\n" +
                "(get-value (products_PRICE))\n";

        assertEquals(expected, text);
    }
    @Test
    public void productOrPriceParsed() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression("products", CheckExpressionFrom("(\"STOCK\" >= 5 OR \"STOCK\" = 100)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-fun products_STOCK () Int)\n" +
                "(assert (or (>= products_STOCK 5) (= products_STOCK 100)))\n" +
                "(check-sat)\n" +
                "(get-value (products_STOCK))\n";

        assertEquals(expected, text);
    }


    @Test
    public void productGreaterPriceAndStock() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression("products", CheckExpressionFrom("(\"PRICE\">1000)"));
        succeed = succeed && writer.addTableCheckExpression("products", CheckExpressionFrom("(\"STOCK\">=5)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-fun products_STOCK () Int)\n" +
                "(declare-fun products_PRICE () Int)\n" +
                "(assert (> products_PRICE 1000))\n" +
                "(assert (>= products_STOCK 5))\n" +
                "(check-sat)\n" +
                "(get-value (products_STOCK))\n" +
                "(get-value (products_PRICE))\n";

        assertEquals(expected, text);
    }

    @Test
    public void productPriceEnum() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression("products", CheckExpressionFrom("(\"PRICE\" IN (12, 13, 14))"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-fun products_PRICE () Int)\n" +
                "(assert (or (= products_PRICE 14) (or (= products_PRICE 13) (= products_PRICE 12))))\n" +
                "(check-sat)\n" +
                "(get-value (products_PRICE))\n";

        assertEquals(expected, text);
    }

    @NotNull
    private TableCheckExpressionDto CheckExpressionFrom(String checkPriceString) {
        TableCheckExpressionDto checkExpression = new TableCheckExpressionDto();
        checkExpression.sqlCheckExpression = checkPriceString;
        return checkExpression;
    }

}

