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
        boolean succeed = writer.addTableCheckExpression(CheckExpressionFrom("(\"PRICE\" > 100)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-const PRICE Int)\n" +
                "(assert (> PRICE 100))\n" +
                "(check-sat)\n" +
                "(get-value (PRICE))\n";

        assertEquals(expected, text);
    }

    @Test
    public void productGreaterAndLowerPriceAsParsed() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression(CheckExpressionFrom("(\"PRICE\" > 100 AND \"PRICE\" < 9999)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-const PRICE Int)\n" +
                "(assert (and (> PRICE 100) (< PRICE 9999)))\n" +
                "(check-sat)\n" +
                "(get-value (PRICE))\n";

        assertEquals(expected, text);
    }
    @Test
    public void productOrPriceParsed() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression(CheckExpressionFrom("(\"STOCK\" >= 5 OR \"STOCK\" = 100)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-const STOCK Int)\n" +
                "(assert (or (>= STOCK 5) (= STOCK 100)))\n" +
                "(check-sat)\n" +
                "(get-value (STOCK))\n";

        assertEquals(expected, text);
    }


    @Test
    public void productGreaterPriceAndStock() {
        Smt2Writer writer = new Smt2Writer(DatabaseType.H2);
        boolean succeed = writer.addTableCheckExpression(CheckExpressionFrom("(\"PRICE\">1000)"));
        succeed = succeed && writer.addTableCheckExpression(CheckExpressionFrom("(\"STOCK\">=5)"));

        assertTrue(succeed);

        String text = writer.asText();

        String expected = "(set-logic QF_SLIA)\n" +
                "(declare-const PRICE Int)\n" +
                "(declare-const STOCK Int)\n" +
                "(assert (> PRICE 1000))\n" +
                "(assert (>= STOCK 5))\n" +
                "(check-sat)\n" +
                "(get-value (PRICE))\n" +
                "(get-value (STOCK))\n";

        assertEquals(expected, text);
    }

    @NotNull
    private TableCheckExpressionDto CheckExpressionFrom(String checkPriceString) {
        TableCheckExpressionDto checkExpression = new TableCheckExpressionDto();
        checkExpression.sqlCheckExpression = checkPriceString;
        return checkExpression;
    }

}

