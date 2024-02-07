import org.evomaster.client.java.controller.api.dto.database.schema.TableCheckExpressionDto;
import org.evomaster.client.java.sql.internal.constraint.DbTableCheckExpression;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Smt2WriterTest {

    // ************************************** //
    // ********** CHECK constraint ********** //
    // ************************************** //
    @Test
    public void productGreaterPriceAsParsed() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addTableCheckExpression(CheckExpressionFrom("(\"PRICE\" > 100)"));

        assertTrue(succeed);

        String text = writer.asText();

        assertEquals(expectedSmt2(">", "100"), text);
    }

    // TODO: Add support for multiple check constraints
    @Test
    @Disabled
    public void productGreaterPriceAndStock() {
        Smt2Writer writer = new Smt2Writer();
        boolean succeed = writer.addTableCheckExpression(CheckExpressionFrom("(\"PRICE\">1000 AND \"STOCK\">5)"));

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
                "(declare-const PRICE Int)\n" +
                "(assert (" + cmp + " PRICE " + val + "))\n" +
                "(check-sat)\n" +
                "(get-value (PRICE))\n";
    }

    @NotNull
    private TableCheckExpressionDto CheckExpressionFrom(String checkPriceString) {
        TableCheckExpressionDto checkExpression = new TableCheckExpressionDto();
        checkExpression.sqlCheckExpression = checkPriceString;
        return checkExpression;
    }

}

