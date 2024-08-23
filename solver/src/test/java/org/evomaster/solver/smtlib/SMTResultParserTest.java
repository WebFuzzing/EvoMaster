package org.evomaster.solver.smtlib;

import org.evomaster.solver.smtlib.value.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SMTResultParserTest {
    @Test
    public void testParseEmptyResponse() {
        String response = "";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseMalformedResponse() {
        String response = "sat\n(id_1 2)";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseSimpleIntValue() {
        String response = "sat\n((id_1 2))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("id_1") instanceof IntValue);
        assertEquals(2, ((IntValue) result.get("id_1")).getValue());
    }

    @Test
    public void testParseSimpleStringValue() {
        String response = "sat\n((name_1 \"example\"))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("name_1") instanceof StringValue);
        assertEquals("example", ((StringValue) result.get("name_1")).getValue());
    }

    @Test
    public void testParseSimpleRealValue() {
        String response = "sat\n((pi_1 3.14))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("pi_1") instanceof RealValue);
        assertEquals(3.14, ((RealValue) result.get("pi_1")).getValue());
    }

    @Test
    public void testParseNegativeValue() {
        String response = "sat\n" +
                "((y 0))\n" +
                "((x (- 4)))";
        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(2, result.size());
        assertTrue(result.get("y") instanceof IntValue);
        assertEquals(0, ((IntValue) result.get("y")).getValue());
        assertTrue(result.get("x") instanceof IntValue);
        assertEquals(-4, ((IntValue) result.get("x")).getValue());
    }

    @Test
    public void testParseComposedType() {
        String response = "sat\n((users1 (id-document-name-age-points 4 2 \"agus\" 31 7)))";

        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);

        assertEquals(1, result.size());
        assertTrue(result.get("users1") instanceof StructValue);
        StructValue users1 = (StructValue) result.get("users1");
        assertEquals(5, users1.getFields().size());
        assertTrue(users1.getField("ID") instanceof IntValue);
        assertEquals(4, ((IntValue) users1.getField("ID")).getValue());
        assertTrue(users1.getField("DOCUMENT") instanceof IntValue);
        assertEquals(2, ((IntValue) users1.getField("DOCUMENT")).getValue());
        assertTrue(users1.getField("NAME") instanceof StringValue);
        assertEquals("agus", ((StringValue) users1.getField("NAME")).getValue());
        assertTrue(users1.getField("AGE") instanceof IntValue);
        assertEquals(31, ((IntValue) users1.getField("AGE")).getValue());
        assertTrue(users1.getField("POINTS") instanceof IntValue);
        assertEquals(7, ((IntValue) users1.getField("POINTS")).getValue());
    }

    @Test
    public void testParseMultipleEntries() {
        String response = "sat\n" +
                "((products1 (price-min_price-stock-user_id 5 501 8 4)))\n" +
                "((products2 (price-min_price-stock-user_id 9 21739 8 6)))\n" +
                "((users1 (id-document-name-age-points 4 2 \"agus\" 31 7)))\n" +
                "((users2 (id-document-name-age-points 6 3 \"agus\" 91 7)))\n";

        Map<String, SMTLibValue> result = SMTResultParser.parseZ3Response(response);
        assertEquals(4, result.size());
        assertTrue(result.get("products1") instanceof StructValue);
        StructValue products1 = (StructValue) result.get("products1");
        assertEquals(4, products1.getFields().size());
        assertTrue(products1.getField("PRICE") instanceof IntValue);
        assertEquals(5, ((IntValue) products1.getField("PRICE")).getValue());
        assertTrue(products1.getField("MIN_PRICE") instanceof IntValue);
        assertEquals(501, ((IntValue) products1.getField("MIN_PRICE")).getValue());
        assertTrue(products1.getField("STOCK") instanceof IntValue);
        assertEquals(8, ((IntValue) products1.getField("STOCK")).getValue());
        assertTrue(products1.getField("USER_ID") instanceof IntValue);
        assertEquals(4, ((IntValue) products1.getField("USER_ID")).getValue());

        assertTrue(result.get("products2") instanceof StructValue);
        StructValue products2 = (StructValue) result.get("products2");
        assertEquals(4, products2.getFields().size());
        assertTrue(products2.getField("PRICE") instanceof IntValue);
        assertEquals(9, ((IntValue) products2.getField("PRICE")).getValue());
        assertTrue(products2.getField("MIN_PRICE") instanceof IntValue);
        assertEquals(21739, ((IntValue) products2.getField("MIN_PRICE")).getValue());
        assertTrue(products2.getField("STOCK") instanceof IntValue);
        assertEquals(8, ((IntValue) products2.getField("STOCK")).getValue());
        assertTrue(products2.getField("USER_ID") instanceof IntValue);
        assertEquals(6, ((IntValue) products2.getField("USER_ID")).getValue());

        assertTrue(result.get("users1") instanceof StructValue);
        StructValue users1 = (StructValue) result.get("users1");
        assertEquals(5, users1.getFields().size());
        assertTrue(users1.getField("ID") instanceof IntValue);
        assertEquals(4, ((IntValue) users1.getField("ID")).getValue());
        assertTrue(users1.getField("DOCUMENT") instanceof IntValue);
        assertEquals(2, ((IntValue) users1.getField("DOCUMENT")).getValue());
        assertTrue(users1.getField("NAME") instanceof StringValue);
        assertEquals("agus", ((StringValue) users1.getField("NAME")).getValue());
        assertTrue(users1.getField("AGE") instanceof IntValue);
        assertEquals(31, ((IntValue) users1.getField("AGE")).getValue());
        assertTrue(users1.getField("POINTS") instanceof IntValue);
        assertEquals(7, ((IntValue) users1.getField("POINTS")).getValue());

        assertTrue(result.get("users2") instanceof StructValue);
        StructValue users2 = (StructValue) result.get("users2");
        assertEquals(5, users2.getFields().size());
        assertTrue(users2.getField("ID") instanceof IntValue);
        assertEquals(6, ((IntValue) users2.getField("ID")).getValue());
        assertTrue(users2.getField("DOCUMENT") instanceof IntValue);
        assertEquals(3, ((IntValue) users2.getField("DOCUMENT")).getValue());
        assertTrue(users2.getField("NAME") instanceof StringValue);
        assertEquals("agus", ((StringValue) users2.getField("NAME")).getValue());
        assertTrue(users2.getField("AGE") instanceof IntValue);
        assertEquals(91, ((IntValue) users2.getField("AGE")).getValue());
        assertTrue(users2.getField("POINTS") instanceof IntValue);
        assertEquals(7, ((IntValue) users2.getField("POINTS")).getValue());
    }
}
