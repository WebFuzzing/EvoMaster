package com.webfuzzing.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.webfuzzing.asyncapi.mapper.AsyncApiMapper;
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for reference handling, at a finer grain than a whole document allows.
 */
public class AsyncApiRefResolverTest {

    private JsonNode document() throws IOException {
        return AsyncApiMapper.readTree(
                "asyncapi: 3.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    placed:\n"
                        + "      name: OrderPlaced\n"
                        + "      payload:\n"
                        + "        type: object\n"
                        + "        properties:\n"
                        + "          item:\n"
                        + "            $ref: '#/components/schemas/with~1slash'\n"
                        + "          history:\n"
                        + "            type: array\n"
                        + "            items:\n"
                        + "              $ref: '#/components/schemas/with~0tilde'\n"
                        + "  schemas:\n"
                        + "    with/slash:\n"
                        + "      type: string\n"
                        + "    with~tilde:\n"
                        + "      type: integer\n");
    }

    @Test
    public void testLocalReferenceIsFollowed() throws IOException {

        JsonNode resolved = AsyncApiRefResolver.resolveLocal(document(), "#/components/messages/placed");

        assertNotNull(resolved);
        assertEquals("OrderPlaced", resolved.get("name").asText());
    }

    @Test
    public void testReferenceToTheDocumentRootIsTheDocument() throws IOException {
        JsonNode document = document();
        assertEquals(document, AsyncApiRefResolver.resolveLocal(document, "#"));
    }

    @Test
    public void testReferenceThatLeadsNowhere() throws IOException {
        assertNull(AsyncApiRefResolver.resolveLocal(document(), "#/components/messages/absent"));
        assertNull(AsyncApiRefResolver.resolveLocal(document(), "#/nothing/here/at/all"));
    }

    @Test
    public void testReferenceToAnotherDocumentIsNotLocal() throws IOException {

        assertFalse(AsyncApiRefResolver.isLocal("other.yaml#/components/schemas/Order"));
        assertNull(AsyncApiRefResolver.resolveLocal(document(), "other.yaml#/components/messages/placed"));
    }

    @Test
    public void testEscapedPointerSegments() throws IOException {

        //JSON Pointer escapes '/' as '~1' and '~' as '~0'
        assertEquals("string", AsyncApiRefResolver
                .resolveLocal(document(), "#/components/schemas/with~1slash").get("type").asText());
        assertEquals("integer", AsyncApiRefResolver
                .resolveLocal(document(), "#/components/schemas/with~0tilde").get("type").asText());
    }

    @Test
    public void testKeyOfAReferenceWithTheExpectedShape() {

        assertEquals(
                "placed",
                AsyncApiRefResolver.refKey("#/components/messages/placed", "#/components/messages/"));
    }

    @Test
    public void testKeyOfAReferenceWithAnotherShape() {

        //a pointer into a channel is not a component message, however similar it looks
        assertNull(AsyncApiRefResolver.refKey(
                "#/channels/orders/messages/placed", "#/components/messages/"));
        //nor is a deeper pointer at the expected place
        assertNull(AsyncApiRefResolver.refKey(
                "#/components/messages/placed/payload", "#/components/messages/"));
        assertNull(AsyncApiRefResolver.refKey("#/components/messages/", "#/components/messages/"));
    }

    @Test
    public void testSchemaKeyOfAReference() {

        //unlike refKey, a pointer that goes deeper still names the schema it goes into
        assertEquals("Order", AsyncApiRefResolver.schemaKeyOf("#/components/schemas/Order"));
        assertEquals("Order", AsyncApiRefResolver.schemaKeyOf("#/components/schemas/Order/properties/item"));
        assertEquals("with/slash", AsyncApiRefResolver.schemaKeyOf("#/components/schemas/with~1slash"));

        assertNull(AsyncApiRefResolver.schemaKeyOf("#/definitions/Order"));
        assertNull(AsyncApiRefResolver.schemaKeyOf("#/components/messages/order"));
        assertNull(AsyncApiRefResolver.schemaKeyOf("other.yaml#/components/schemas/Order"));
        assertNull(AsyncApiRefResolver.schemaKeyOf("#/components/schemas/"));
    }

    @Test
    public void testReferencesAreFoundAtAnyDepth() throws IOException {

        List<String> refs = AsyncApiRefResolver.collectRefs(document());

        //one nested three levels inside a schema, one further down inside an array
        assertEquals(
                Arrays.asList("#/components/schemas/with~1slash", "#/components/schemas/with~0tilde"),
                refs);
    }

    @Test
    public void testRefOfSomethingThatIsNotAReference() throws IOException {

        JsonNode document = document();

        assertNull(AsyncApiRefResolver.refOf(null));
        assertNull(AsyncApiRefResolver.refOf(document.get("components")));
        assertEquals(
                "#/components/schemas/with~1slash",
                AsyncApiRefResolver.refOf(document.get("components").get("messages").get("placed")
                        .get("payload").get("properties").get("item")));
    }

}
