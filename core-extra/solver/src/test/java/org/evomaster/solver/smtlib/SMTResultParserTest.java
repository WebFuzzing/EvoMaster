package org.evomaster.solver.smtlib;

import org.evomaster.solver.Z3Solution;
import org.evomaster.solver.smtlib.value.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SMTResultParserTest {
    @Test
    public void testParseEmptyResponse() {
        String response = "";
        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseMalformedResponse() {
        String response = "sat\n(id_1 2)";
        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseSimpleIntValue() {
        String response = "sat\n((id_1 2))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("id_1") instanceof LongValue);
        assertEquals(2, ((LongValue) result.get("id_1")).getValue());
    }

    @Test
    public void testParseSimpleStringValue() {
        String response = "sat\n((name_1 \"example\"))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("name_1") instanceof StringValue);
        assertEquals("example", ((StringValue) result.get("name_1")).getValue());
    }

    @Test
    public void testParseSimpleRealValue() {
        String response = "sat\n((pi_1 3.14))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertEquals(1, result.size());
        assertTrue(result.get("pi_1") instanceof RealValue);
        assertEquals(3.14, ((RealValue) result.get("pi_1")).getValue());
    }

    @Test
    public void testParseNegativeValue() {
        String response = "sat\n" +
                "((y 0))\n" +
                "((x (- 4)))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertEquals(2, result.size());
        assertTrue(result.get("y") instanceof LongValue);
        assertEquals(0, ((LongValue) result.get("y")).getValue());
        assertTrue(result.get("x") instanceof LongValue);
        assertEquals(-4, ((LongValue) result.get("x")).getValue());
    }

    @Test
    public void testParseComposedType() {
        String response = "sat\n((users1 (id-document-name-age-points 4 2 \"Alice\" 31 7)))";

        Z3Solution result = SMTResultParser.parseZ3Response(response);

        assertEquals(1, result.size());
        assertTrue(result.get("users1") instanceof StructValue);
        StructValue users1 = (StructValue) result.get("users1");
        assertEquals(5, users1.getFields().size());
        assertTrue(users1.getField("ID") instanceof LongValue);
        assertEquals(4, ((LongValue) users1.getField("ID")).getValue());
        assertTrue(users1.getField("DOCUMENT") instanceof LongValue);
        assertEquals(2, ((LongValue) users1.getField("DOCUMENT")).getValue());
        assertTrue(users1.getField("NAME") instanceof StringValue);
        assertEquals("Alice", ((StringValue) users1.getField("NAME")).getValue());
        assertTrue(users1.getField("AGE") instanceof LongValue);
        assertEquals(31, ((LongValue) users1.getField("AGE")).getValue());
        assertTrue(users1.getField("POINTS") instanceof LongValue);
        assertEquals(7, ((LongValue) users1.getField("POINTS")).getValue());
    }

    @Test
    public void testParseMultipleEntries() {
        String response = "sat\n" +
                "((products1 (price-min_price-stock-user_id 5 501 8 4)))\n" +
                "((products2 (price-min_price-stock-user_id 9 21739 8 6)))\n" +
                "((users1 (id-document-name-age-points 4 2 \"Alice\" 31 7)))\n" +
                "((users2 (id-document-name-age-points 6 3 \"Bob\" 91 7)))\n";

        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertEquals(4, result.size());

        assertTrue(result.get("products1") instanceof StructValue);
        StructValue products1 = (StructValue) result.get("products1");
        assertEquals(4, products1.getFields().size());
        assertTrue(products1.getField("PRICE") instanceof LongValue);
        assertEquals(5, ((LongValue) products1.getField("PRICE")).getValue());
        assertTrue(products1.getField("MIN_PRICE") instanceof LongValue);
        assertEquals(501, ((LongValue) products1.getField("MIN_PRICE")).getValue());
        assertTrue(products1.getField("STOCK") instanceof LongValue);
        assertEquals(8, ((LongValue) products1.getField("STOCK")).getValue());
        assertTrue(products1.getField("USER_ID") instanceof LongValue);
        assertEquals(4, ((LongValue) products1.getField("USER_ID")).getValue());

        assertTrue(result.get("products2") instanceof StructValue);
        StructValue products2 = (StructValue) result.get("products2");
        assertEquals(4, products2.getFields().size());
        assertTrue(products2.getField("PRICE") instanceof LongValue);
        assertEquals(9, ((LongValue) products2.getField("PRICE")).getValue());
        assertTrue(products2.getField("MIN_PRICE") instanceof LongValue);
        assertEquals(21739, ((LongValue) products2.getField("MIN_PRICE")).getValue());
        assertTrue(products2.getField("STOCK") instanceof LongValue);
        assertEquals(8, ((LongValue) products2.getField("STOCK")).getValue());
        assertTrue(products2.getField("USER_ID") instanceof LongValue);
        assertEquals(6, ((LongValue) products2.getField("USER_ID")).getValue());

        assertTrue(result.get("users1") instanceof StructValue);
        StructValue users1 = (StructValue) result.get("users1");
        assertEquals(5, users1.getFields().size());
        assertTrue(users1.getField("ID") instanceof LongValue);
        assertEquals(4, ((LongValue) users1.getField("ID")).getValue());
        assertTrue(users1.getField("DOCUMENT") instanceof LongValue);
        assertEquals(2, ((LongValue) users1.getField("DOCUMENT")).getValue());
        assertTrue(users1.getField("NAME") instanceof StringValue);
        assertEquals("Alice", ((StringValue) users1.getField("NAME")).getValue());
        assertTrue(users1.getField("AGE") instanceof LongValue);
        assertEquals(31, ((LongValue) users1.getField("AGE")).getValue());
        assertTrue(users1.getField("POINTS") instanceof LongValue);
        assertEquals(7, ((LongValue) users1.getField("POINTS")).getValue());

        assertTrue(result.get("users2") instanceof StructValue);
        StructValue users2 = (StructValue) result.get("users2");
        assertEquals(5, users2.getFields().size());
        assertTrue(users2.getField("ID") instanceof LongValue);
        assertEquals(6, ((LongValue) users2.getField("ID")).getValue());
        assertTrue(users2.getField("DOCUMENT") instanceof LongValue);
        assertEquals(3, ((LongValue) users2.getField("DOCUMENT")).getValue());
        assertTrue(users2.getField("NAME") instanceof StringValue);
        assertEquals("Bob", ((StringValue) users2.getField("NAME")).getValue());
        assertTrue(users2.getField("AGE") instanceof LongValue);
        assertEquals(91, ((LongValue) users2.getField("AGE")).getValue());
        assertTrue(users2.getField("POINTS") instanceof LongValue);
        assertEquals(7, ((LongValue) users2.getField("POINTS")).getValue());
    }

    
    @Test
    public void testNegativeLong() {
        String response = "sat\n" +
                "((documents_specs1 (id-first_page-last_page-printing_schema-document_id 5 6 7 8 (- 4194297))))\n" +
                "((printing_schemas1 (id-binding_specs-cover_specs-is_deleted-pschema_name-paper_specs-consumer_id\n" +
                "  8\n" +
                "  \"true\"\n" +
                "  \"true\"\n" +
                "  \"true\"\n" +
                "  \"true\"\n" +
                "  \"true\"\n" +
                "  2)))\n";

        Z3Solution result = SMTResultParser.parseZ3Response(response);
        assertEquals(2, result.size());

        assertTrue(result.get("documents_specs1") instanceof StructValue);
        StructValue documents_specs1 = (StructValue) result.get("documents_specs1");
        assertEquals(5, documents_specs1.getFields().size());
        assertTrue(documents_specs1.getField("ID") instanceof LongValue);
        assertEquals(5, ((LongValue) documents_specs1.getField("ID")).getValue());
        assertTrue(documents_specs1.getField("FIRST_PAGE") instanceof LongValue);
        assertEquals(6, ((LongValue) documents_specs1.getField("FIRST_PAGE")).getValue());
        assertTrue(documents_specs1.getField("LAST_PAGE") instanceof LongValue);
        assertEquals(7, ((LongValue) documents_specs1.getField("LAST_PAGE")).getValue());
        assertTrue(documents_specs1.getField("PRINTING_SCHEMA") instanceof LongValue);
        assertEquals(8, ((LongValue) documents_specs1.getField("PRINTING_SCHEMA")).getValue());
        assertTrue(documents_specs1.getField("DOCUMENT_ID") instanceof LongValue);
        assertEquals(-4194297, ((LongValue) documents_specs1.getField("DOCUMENT_ID")).getValue());

        assertTrue(result.get("printing_schemas1") instanceof StructValue);
        StructValue printing_schemas1 = (StructValue) result.get("printing_schemas1");
        assertEquals(7, printing_schemas1.getFields().size());
        assertTrue(printing_schemas1.getField("ID") instanceof LongValue);
        assertEquals(8, ((LongValue) printing_schemas1.getField("ID")).getValue());
        assertTrue(printing_schemas1.getField("BINDING_SPECS") instanceof StringValue);
        assertEquals("true", ((StringValue) printing_schemas1.getField("BINDING_SPECS")).getValue());
        assertTrue(printing_schemas1.getField("COVER_SPECS") instanceof StringValue);
        assertEquals("true", ((StringValue) printing_schemas1.getField("COVER_SPECS")).getValue());
        assertTrue(printing_schemas1.getField("IS_DELETED") instanceof StringValue);
        assertEquals("true", ((StringValue) printing_schemas1.getField("IS_DELETED")).getValue());
        assertTrue(printing_schemas1.getField("PSCHEMA_NAME") instanceof StringValue);
        assertEquals("true", ((StringValue) printing_schemas1.getField("PSCHEMA_NAME")).getValue());
        assertTrue(printing_schemas1.getField("PAPER_SPECS") instanceof StringValue);
        assertEquals("true", ((StringValue) printing_schemas1.getField("PAPER_SPECS")).getValue());
        assertTrue(printing_schemas1.getField("CONSUMER_ID") instanceof LongValue);
        assertEquals(2, ((LongValue) printing_schemas1.getField("CONSUMER_ID")).getValue());
    }

    @Test
    public void testParseComposedTypeWithSpaceInString() {
        String response = "sat\n((users__1 (id-name-age 2 \"John Smith\" 26)))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);

        StructValue users1 = (StructValue) result.get("users__1");
        assertEquals(3, users1.getFields().size());
        assertEquals(2, ((LongValue) users1.getField("ID")).getValue());
        assertEquals("John Smith", ((StringValue) users1.getField("NAME")).getValue());
        assertEquals(26, ((LongValue) users1.getField("AGE")).getValue());
    }

    @Test
    public void testParseComposedTypeWithParenthesisInString() {
        String response = "sat\n((t__1 (id-txt-n 0 \"a)b (c\" 7)))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);

        StructValue t1 = (StructValue) result.get("t__1");
        assertEquals(3, t1.getFields().size());
        assertEquals(0, ((LongValue) t1.getField("ID")).getValue());
        assertEquals("a)b (c", ((StringValue) t1.getField("TXT")).getValue());
        assertEquals(7, ((LongValue) t1.getField("N")).getValue());
    }

    @Test
    public void testParseComposedTypeWithNegativeValueAndMultilineString() {
        String response = "sat\n((t__1 (id-txt-n (- 3)\n  \"x y\"\n  7)))\n((t__2 (id-txt-n 1 \"z\" 2)))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);

        StructValue t1 = (StructValue) result.get("t__1");
        assertEquals(-3, ((LongValue) t1.getField("ID")).getValue());
        assertEquals("x y", ((StringValue) t1.getField("TXT")).getValue());
        assertEquals(7, ((LongValue) t1.getField("N")).getValue());
        StructValue t2 = (StructValue) result.get("t__2");
        assertEquals("z", ((StringValue) t2.getField("TXT")).getValue());
    }

    /**
     * Two tables with the same columns share a constructor name, and Z3 then qualifies it with its sort.
     * This is the exact output of the Z3 image for two such tables.
     */
    @Test
    public void testParseComposedTypeWithQualifiedConstructor() {
        String response = "sat\n((doc__1 ((as id-body DocRow) (- 2) \"\")))\n((note__1 ((as id-body NoteRow) 1 \"a b\")))";
        Z3Solution result = SMTResultParser.parseZ3Response(response);

        StructValue doc1 = (StructValue) result.get("doc__1");
        assertEquals(2, doc1.getFields().size());
        assertEquals(-2, ((LongValue) doc1.getField("ID")).getValue());
        assertEquals("", ((StringValue) doc1.getField("BODY")).getValue());
        StructValue note1 = (StructValue) result.get("note__1");
        assertEquals(1, ((LongValue) note1.getField("ID")).getValue());
        assertEquals("a b", ((StringValue) note1.getField("BODY")).getValue());
    }
}
