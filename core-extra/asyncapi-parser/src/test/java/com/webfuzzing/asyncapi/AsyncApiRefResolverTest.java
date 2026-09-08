package com.webfuzzing.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.webfuzzing.asyncapi.mapper.AsyncApiMapper;
import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.models.DocumentLocationType;
import com.webfuzzing.asyncapi.parser.AsyncApiParser;
import com.webfuzzing.asyncapi.resolver.AsyncApiDocumentFetcher;
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver;
import com.webfuzzing.asyncapi.resolver.RefLocations;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                        + "      type: integer\n"
                        + "    bad%zz:\n"
                        + "      type: boolean\n");
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
    public void testPercentEncodedPointerSegmentsAreDecoded() throws IOException {

        //references are URIs, so a key may arrive percent-encoded as well as pointer-escaped
        assertEquals("string", AsyncApiRefResolver
                .resolveLocal(document(), "#/components/schemas/with%2Fslash").get("type").asText());

        /*
            "%zz" is not a valid escape. Rather than failing, the segment is taken as written --
            and here that is a key which exists, so the fallback is observable.
         */
        assertEquals("boolean", AsyncApiRefResolver
                .resolveLocal(document(), "#/components/schemas/bad%zz").get("type").asText());
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

    // ------------------------------------------------------------------ where a reference leads

    @Test
    public void testAnAbsoluteReferenceIsTakenAsItIs() {

        List<String> messages = new ArrayList<>();

        assertEquals(
                "https://example.com/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "https://example.com/shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofLocal("/some/where/main.yaml"),
                        messages));

        //plain http is absolute just the same
        assertEquals(
                "http://example.com/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "http://example.com/shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofLocal("/some/where/main.yaml"),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testARelativeReferenceIsResolvedAgainstTheReferringDocument() {

        List<String> messages = new ArrayList<>();

        assertEquals(
                "/some/where/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofLocal("/some/where/main.yaml"),
                        messages));

        //a document one directory down means that directory, not the primary document's
        assertEquals(
                "/some/where/sub/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofLocal("/some/where/sub/nested.yaml"),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testDotAndDotDotSegmentsAreCollapsed() {

        List<String> messages = new ArrayList<>();

        /*
            The resolved location is what tells imported documents apart, so two references to
            the same document must come out as the same string however they were written. A
            plain path is resolved through the file system and a URL per RFC 3986; both collapse
            these segments.
         */
        assertEquals(
                "/a/common/x.yaml",
                RefLocations.resolveDocumentLocation(
                        "../common/x.yaml#/components/schemas/X",
                        DocumentLocation.ofLocal("/a/b/main.yaml"),
                        messages));
        assertEquals(
                "/a/b/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "./shared.yaml#/components/schemas/X",
                        DocumentLocation.ofLocal("/a/b/main.yaml"),
                        messages));
        assertEquals(
                "https://example.com/common/x.yaml",
                RefLocations.resolveDocumentLocation(
                        "../common/x.yaml#/components/schemas/X",
                        DocumentLocation.ofRemote("https://example.com/a/main.yaml"),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testAPathWithASpaceIsNotAUriAndIsResolvedAsAPath() {

        List<String> messages = new ArrayList<>();

        //java.net.URI would reject this; a path is resolved through the file system instead
        assertEquals(
                "/has space/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofLocal("/has space/main.yaml"),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testADocumentServedFromADirectoryUrl() {

        List<String> messages = new ArrayList<>();

        //a trailing slash means the URL is the folder itself, so nothing is stripped from it
        assertEquals(
                "https://example.com/docs/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofRemote("https://example.com/docs/"),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testAFileUrlIsResolvedAsAUrl() {

        List<String> messages = new ArrayList<>();

        String resolved = RefLocations.resolveDocumentLocation(
                "shared.yaml#/components/schemas/Thing",
                DocumentLocation.ofLocal("file:///a/b/main.yaml"),
                messages);

        //URI is free to write one slash or three after "file:"; both name the same file
        assertTrue(resolved.startsWith("file:"), resolved);
        assertTrue(resolved.endsWith("/a/b/shared.yaml"), resolved);
        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testAReferringUrlThatIsNotAValidUriIsReported() {

        List<String> messages = new ArrayList<>();

        //a URL is a URI, and one with a space in it is not one; there is no right answer here
        assertNull(RefLocations.resolveDocumentLocation(
                "shared.yaml#/components/schemas/Thing",
                DocumentLocation.ofRemote("https://example.com/has space/main.yaml"),
                messages));

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("not a valid URI"), messages.toString());
    }

    @Test
    public void testAProtocolRelativeReferenceBorrowsTheProtocol() {

        List<String> messages = new ArrayList<>();

        assertEquals(
                "https://other.com/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "//other.com/shared.yaml#/components/schemas/Thing",
                        DocumentLocation.ofRemote("https://example.com/main.yaml"),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    public void testAProtocolRelativeReferenceWithNoProtocolToBorrow() {

        List<String> messages = new ArrayList<>();

        /*
            A plain file path has no protocol, so there is nothing to borrow. Reporting it is
            the only sensible answer -- taking the text apart regardless would read past the
            start of the string.
         */
        assertNull(RefLocations.resolveDocumentLocation(
                "//other.com/shared.yaml#/components/schemas/Thing",
                DocumentLocation.ofLocal("/some/where/main.yaml"),
                messages));

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("No protocol"), messages.toString());
    }

    @Test
    public void testAReferenceWithNoFragmentIsNotOne() {

        List<String> messages = new ArrayList<>();

        assertNull(RefLocations.resolveDocumentLocation(
                "shared.yaml", DocumentLocation.ofLocal("/some/where/main.yaml"), messages));

        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("contains no #"), messages.toString());
    }

    @Test
    public void testARelativeReferenceFromADocumentWithNoLocation() {

        //a document handed over as text has no neighbours for a relative reference to name
        assertThrows(IllegalArgumentException.class, () -> RefLocations.resolveDocumentLocation(
                "shared.yaml#/components/schemas/Thing",
                DocumentLocation.MEMORY,
                new ArrayList<String>()));
    }

    @Test
    public void testALocalReferenceIsRecognisedWhateverItPointsAt() {

        assertTrue(RefLocations.isLocalRef("#/components/schemas/Thing"));
        assertTrue(RefLocations.isLocalRef("#"));
        assertFalse(RefLocations.isLocalRef("shared.yaml#/components/schemas/Thing"));
        assertFalse(RefLocations.isLocalRef("https://example.com/shared.yaml#/x"));
    }

    @Test
    public void testWhereADocumentReadFromTheClasspathLooksForItsNeighbours() {

        List<String> messages = new ArrayList<>();

        //a resource path is resolved the same way a file path is
        assertEquals(
                "/asyncapi/artificial/shared.yaml",
                RefLocations.resolveDocumentLocation(
                        "shared.yaml#/components/schemas/Thing",
                        new DocumentLocation("/asyncapi/artificial/main.yaml", DocumentLocationType.RESOURCE),
                        messages));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    // ------------------------------------------------------------------ importing other documents

    private static final String SHARED_THING =
            "components:\n"
                    + "  schemas:\n"
                    + "    Thing:\n"
                    + "      type: string\n";

    private static String mainReferring(String ref) {
        return "asyncapi: 3.0.0\n"
                + "info:\n"
                + "  title: Split across documents\n"
                + "  version: 1.0.0\n"
                + "components:\n"
                + "  messages:\n"
                + "    m:\n"
                + "      payload:\n"
                + "        $ref: '" + ref + "'\n";
    }

    private static int importedSchemas(AsyncApiDocument document) {

        int count = 0;

        for (String key : document.getComponentSchemas().keySet()) {
            if (key.startsWith("_ext_")) {
                count++;
            }
        }

        return count;
    }

    private boolean warns(AsyncApiDocument document, String text) {

        for (String warning : document.getWarnings()) {
            if (warning.contains(text)) {
                return true;
            }
        }

        return false;
    }

    @Test
    public void testAnotherDocumentIsFetchedFromARemoteLocation() {

        List<String> fetched = new ArrayList<>();
        List<DocumentLocationType> fetchedAs = new ArrayList<>();

        /*
            The fetcher is an interface precisely so that this can be tested without a server:
            it is handed the absolute location and told how to read it, and hands back the text.
         */
        AsyncApiDocumentFetcher fetcher = (location, type) -> {
            fetched.add(location);
            fetchedAs.add(type);
            return SHARED_THING;
        };

        AsyncApiDocument document = AsyncApiParser.parse(
                mainReferring("shared.yaml#/components/schemas/Thing"),
                DocumentLocation.ofRemote("https://example.com/api/main.yaml"),
                fetcher);

        //resolved against the referring URL, and read the way a URL is read
        assertEquals(Arrays.asList("https://example.com/api/shared.yaml"), fetched);
        assertEquals(Arrays.asList(DocumentLocationType.REMOTE), fetchedAs);

        assertTrue(document.getWarnings().isEmpty(), document.getWarnings().toString());
        assertEquals(1, importedSchemas(document));
        assertTrue(document.getMessages().containsKey("m"));
    }

    @Test
    public void testNoMoreThanAHundredDocumentsAreImported() {

        /*
            References are paths, and a server that answers every path -- or a symlink loop --
            would be followed for ever without a ceiling. Here every location resolves to a
            document, and the message points at one more of them than the ceiling allows.
         */
        StringBuilder main = new StringBuilder(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Too many neighbours\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    m:\n"
                        + "      payload:\n"
                        + "        type: object\n"
                        + "        properties:\n");

        for (int i = 0; i <= 100; i++) {
            main.append("          p").append(i).append(":\n")
                    .append("            $ref: 'doc").append(i).append(".yaml#/components/schemas/Thing'\n");
        }

        AsyncApiDocument document = AsyncApiParser.parse(
                main.toString(),
                DocumentLocation.ofRemote("https://example.com/main.yaml"),
                (location, type) -> SHARED_THING);

        assertEquals(100, importedSchemas(document));
        assertTrue(warns(document, "More than 100 documents"), document.getWarnings().toString());

        //the reference past the ceiling was left unresolved, and the message depending on it goes
        assertFalse(document.getMessages().containsKey("m"));
    }

    @Test
    public void testAReferenceToAWholeDocumentIsReported() {

        //nothing after the '#': it names the other document itself rather than a component of it
        AsyncApiDocument document = AsyncApiParser.parse(
                mainReferring("shared.yaml#"),
                DocumentLocation.ofRemote("https://example.com/main.yaml"),
                (location, type) -> SHARED_THING);

        assertTrue(warns(document, "whole document"), document.getWarnings().toString());
        assertFalse(document.getMessages().containsKey("m"));
    }

    @Test
    public void testAReferenceWithNoFragmentAtAllIsReportedAndNotFetched() {

        List<String> fetched = new ArrayList<>();

        //not a $ref at all by the specification's definition, so there is nothing to retrieve
        AsyncApiDocument document = AsyncApiParser.parse(
                mainReferring("shared.yaml"),
                DocumentLocation.ofRemote("https://example.com/main.yaml"),
                (location, type) -> {
                    fetched.add(location);
                    return SHARED_THING;
                });

        assertTrue(fetched.isEmpty(), "nothing should have been fetched, but got " + fetched);
        assertTrue(warns(document, "contains no #"), document.getWarnings().toString());
        assertFalse(document.getMessages().containsKey("m"));
    }
}
