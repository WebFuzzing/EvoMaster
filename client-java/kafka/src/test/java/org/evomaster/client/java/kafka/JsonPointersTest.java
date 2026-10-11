package org.evomaster.client.java.kafka;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPointersTest {

    private static final String ID = "cid-1";

    @Test
    public void testTokensAreUnescapedAsTheRfcSays() {

        assertEquals(Arrays.asList("a/b", "m~n", "c"), JsonPointers.tokensOf("/a~1b/m~0n/c"));
        //~01 is an escaped tilde followed by 1, not an escaped slash
        assertEquals(Collections.singletonList("~1"), JsonPointers.tokensOf("/~01"));
    }

    @Test
    public void testEmptyTokensNameTheFieldWithNoName() {

        assertTrue(JsonPointers.tokensOf(null).isEmpty());
        assertTrue(JsonPointers.tokensOf("").isEmpty(), "the empty pointer is the whole document");
        assertEquals(Collections.singletonList(""), JsonPointers.tokensOf("/"));
        assertEquals(Arrays.asList("a", ""), JsonPointers.tokensOf("/a/"));
        assertEquals(Arrays.asList("", "b"), JsonPointers.tokensOf("//b"));

        String json = "{\"\":{\"b\":\"" + ID + "\"},\"a\":{\"\":\"x\"}}";
        assertEquals(ID, JsonPointers.read(json, "//b"));
        assertEquals("x", JsonPointers.read(json, "/a/"));
        assertEquals("{\"\":\"" + ID + "\"}", JsonPointers.write("{}", "/", ID));
    }

    @Test
    public void testWhatDoesNotStartWithASlashIsNoPointer() {

        assertNull(JsonPointers.tokensOf("id"));
        assertNull(JsonPointers.read("{\"id\":\"x\"}", "id"));
        assertEquals("{\"id\":\"x\"}", JsonPointers.write("{\"id\":\"x\"}", "id", ID));
    }

    @Test
    public void testHeaderFieldsFollowTheSchemaParserRules() {
        assertEquals("x-request-id", JsonPointers.headerFieldName("/x-request-id"));
        assertEquals("a/b", JsonPointers.headerFieldName("/a~1b"));
        assertEquals("a~b", JsonPointers.headerFieldName("/a~0b"));
        assertNull(JsonPointers.headerFieldName("/meta/id"));
        assertNull(JsonPointers.headerFieldName("id"));
        assertNull(JsonPointers.headerFieldName("/"));
        assertNull(JsonPointers.headerFieldName(null));
    }

    @Test
    public void testScalarsAreReadAsText() {

        String json = "{\"s\":\"x\\\"y\",\"n\":-1.5e3,\"b\":true,\"nested\":{\"id\":\"" + ID + "\"}}";

        assertEquals("x\"y", JsonPointers.read(json, "/s"));
        assertEquals("-1.5e3", JsonPointers.read(json, "/n"), "a number is read as written");
        assertEquals("true", JsonPointers.read(json, "/b"));
        assertEquals(ID, JsonPointers.read(json, "/nested/id"));
    }

    @Test
    public void testArrayElementsAreReachedByIndex() {

        String json = "{\"list\":[\"a\",{\"id\":\"" + ID + "\"}]}";

        assertEquals("a", JsonPointers.read(json, "/list/0"));
        assertEquals(ID, JsonPointers.read(json, "/list/1/id"));
        assertNull(JsonPointers.read(json, "/list/2"));
        assertNull(JsonPointers.read(json, "/list/-1"));
        assertNull(JsonPointers.read(json, "/list/first"));
        //an index is digits, with no sign and no leading zero
        assertNull(JsonPointers.read(json, "/list/+1"));
        assertNull(JsonPointers.read(json, "/list/01/id"));
        assertNull(JsonPointers.read(json, "/list/99999999999"));
    }

    @Test
    public void testWhatIsNotAScalarReadsAsNothing() {

        String json = "{\"o\":{\"a\":1},\"l\":[1],\"z\":null}";

        assertNull(JsonPointers.read(json, "/o"));
        assertNull(JsonPointers.read(json, "/l"));
        assertNull(JsonPointers.read(json, "/z"), "a JSON null is no correlation id");
        assertNull(JsonPointers.read(json, "/missing"));
        assertNull(JsonPointers.read(json, "/o/a/deeper"));
    }

    @Test
    public void testWhatIsNotJsonReadsAsNothing() {

        for (String notJson : new String[]{null, "", "plain text", "{\"a\":", "{\"a\":1}}", "{\"a\":nope}",
                "{\"a\":\"\\q\"}", "{\"a\":\"\\u12\"}", "{a:1}", "[1 2]"}) {
            assertNull(JsonPointers.read(notJson, "/a"), "read something from: " + notJson);
        }
    }

    @Test
    public void testEscapesAndUnicodeAreDecoded() {

        String json = "{\"k\":\"\\u0041\\n\\t\\/\\\\\\b\\f\\r\"}";

        assertEquals("A\n\t/\\\b\f\r", JsonPointers.read(json, "/k"));
    }

    @Test
    public void testAFieldIsWrittenAndTheRestKeptAsItWas() {

        String json = "{ \"a\" : 1.50, \"b\" : [true, null, \"x\"], \"id\" : \"old\" }";

        String written = JsonPointers.write(json, "/id", ID);

        assertEquals("{\"a\":1.50,\"b\":[true,null,\"x\"],\"id\":\"" + ID + "\"}", written,
                "numbers and literals must go out as written, and field order must be kept");
    }

    @Test
    public void testMissingObjectsAreCreatedOnTheWay() {

        assertEquals("{\"a\":1,\"meta\":{\"trace\":{\"id\":\"" + ID + "\"}}}",
                JsonPointers.write("{\"a\":1}", "/meta/trace/id", ID));
    }

    @Test
    public void testNothingIsOverwrittenToMakeRoom() {

        for (String scalar : new String[]{"3", "\"text\"", "true", "null"}) {
            String json = "{\"meta\":" + scalar + "}";
            assertEquals(json, JsonPointers.write(json, "/meta/id", ID), "overwrote " + scalar);
        }
        String array = "{\"meta\":[1,2]}";
        assertEquals(array, JsonPointers.write(array, "/meta/id", ID), "a field name is no index");
        assertEquals(array, JsonPointers.write(array, "/meta/2", ID), "an element is replaced, never added");
        assertEquals(array, JsonPointers.write(array, "/meta/2/id", ID));
        assertEquals(array, JsonPointers.write(array, "/meta/01", ID));
    }

    @Test
    public void testArraysAreEnteredByIndexWhenWriting() {

        String json = "{\"lines\":[{\"sku\":\"a\"},{\"sku\":\"b\"}],\"tags\":[\"x\",\"y\"]}";

        String nested = JsonPointers.write(json, "/lines/1/id", ID);
        assertEquals("{\"lines\":[{\"sku\":\"a\"},{\"sku\":\"b\",\"id\":\"" + ID + "\"}],\"tags\":[\"x\",\"y\"]}", nested);
        assertEquals(ID, JsonPointers.read(nested, "/lines/1/id"), "what is written must read back the same");

        String element = JsonPointers.write(json, "/tags/0", ID);
        assertEquals(ID, JsonPointers.read(element, "/tags/0"));
        assertEquals("y", JsonPointers.read(element, "/tags/1"));
    }

    @Test
    public void testAWrittenValueIsEscaped() {

        String value = "a\"b\\c\nd\u0001";

        String written = JsonPointers.write("{}", "/id", value);

        assertEquals("{\"id\":\"a\\\"b\\\\c\\nd\\u0001\"}", written);
        assertEquals(value, JsonPointers.read(written, "/id"), "what is written must read back the same");
    }

    @Test
    public void testOnlyAnObjectCanBeWrittenInto() {

        for (String unchanged : new String[]{"[1,2]", "\"text\"", "42", "not json", null}) {
            assertEquals(unchanged, JsonPointers.write(unchanged, "/id", ID), "changed: " + unchanged);
        }
        assertEquals("{\"a\":1}", JsonPointers.write("{\"a\":1}", "", ID), "a pointer naming no field changes nothing");
        assertEquals("{\"a\":1}", JsonPointers.write("{\"a\":1}", null, ID));
    }

    @Test
    public void testEmptyContainersRoundTrip() {

        assertEquals("{\"o\":{},\"l\":[],\"id\":\"" + ID + "\"}",
                JsonPointers.write("{\"o\":{},\"l\":[]}", "/id", ID));
    }

    @Test
    public void testTheValueToWriteCannotBeNull() {

        assertThrows(NullPointerException.class, () -> JsonPointers.write("{}", "/id", null));
    }
}
