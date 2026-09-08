package com.webfuzzing.asyncapi;

import com.webfuzzing.asyncapi.access.AsyncApiAccess;
import com.webfuzzing.asyncapi.models.AsyncApiChannel;
import com.webfuzzing.asyncapi.models.AsyncApiCorrelationId;
import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.models.AsyncApiMessage;
import com.webfuzzing.asyncapi.models.AsyncApiOperation;
import com.webfuzzing.asyncapi.models.AsyncApiReply;
import com.webfuzzing.asyncapi.parser.AsyncApiParsingException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the AsyncAPI 3.x parser.
 *
 * Each document under {@code /asyncapi/artificial} is written to pin down one thing. Several of
 * them exist because a real published document did what they describe: a payload written in
 * Avro, a schema pointing at one that was dropped, references that go round in a circle.
 */
public class AsyncApiParserTest {

    private AsyncApiDocument load(String resourcePath) {
        return AsyncApiAccess.getAsyncApiFromResource(resourcePath);
    }

    private AsyncApiDocument parse(String text) {
        return AsyncApiAccess.parseFromText(text);
    }

    /**
     * Whether any warning mentions the given text, case insensitively.
     */
    private boolean warns(AsyncApiDocument document, String... expected) {

        for (String warning : document.getWarnings()) {

            boolean all = true;

            for (String one : expected) {
                if (!warning.toLowerCase(Locale.ENGLISH).contains(one.toLowerCase(Locale.ENGLISH))) {
                    all = false;
                    break;
                }
            }

            if (all) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> setOf(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private static List<String> namesOf(List<AsyncApiMessage> messages) {

        List<String> names = new ArrayList<>();

        for (AsyncApiMessage message : messages) {
            names.add(message.getName());
        }

        return names;
    }

    // ------------------------------------------------------------------ the shape of a document

    @Test
    public void testMessagesAndTheirSchemas() {

        AsyncApiDocument document = load("/asyncapi/artificial/messages.yaml");

        assertTrue(document.getWarnings().isEmpty(), "unexpected warnings: " + document.getWarnings());
        assertEquals("3.0.0", document.getVersion());
        assertEquals("application/json", document.getDefaultContentType());
        assertEquals(setOf("signupRequest", "signupReply", "heartbeat"), document.getMessages().keySet());
        assertEquals(setOf("SignupRequest", "Address"), document.getComponentSchemas().keySet());

        AsyncApiMessage request = document.getMessages().get("signupRequest");
        assertEquals("SignupRequest", request.getName());
        assertEquals("Sign a user up", request.getTitle());
        //no contentType of its own, so the document's default applies
        assertEquals("application/json", request.getContentType());
        //the payload keeps its reference rather than being inlined
        assertEquals("#/components/schemas/SignupRequest", request.getPayload().get("$ref").asText());

        AsyncApiMessage reply = document.getMessages().get("signupReply");
        assertEquals("application/vnd.example+json", reply.getContentType());
        assertTrue(reply.getPayload().get("properties").has("userId"));

        //a message with no name of its own is known by its component key
        assertEquals("heartbeat", document.getMessages().get("heartbeat").getName());
    }

    @Test
    public void testJsonAndYamlAreParsedTheSameWay() {

        AsyncApiDocument fromYaml = load("/asyncapi/artificial/messages.yaml");
        AsyncApiDocument fromJson = load("/asyncapi/artificial/messages.json");

        //without this, two empty models would compare equal and prove nothing
        assertFalse(fromJson.getMessages().isEmpty());

        assertEquals(fromYaml.getVersion(), fromJson.getVersion());
        assertEquals(fromYaml.getDefaultContentType(), fromJson.getDefaultContentType());
        assertEquals(fromYaml.getMessages().keySet(), fromJson.getMessages().keySet());
        assertEquals(fromYaml.getComponentSchemas().keySet(), fromJson.getComponentSchemas().keySet());

        AsyncApiCorrelationId fromYamlCorrelation =
                fromYaml.getMessages().get("signupRequest").getCorrelationId();
        AsyncApiCorrelationId fromJsonCorrelation =
                fromJson.getMessages().get("signupRequest").getCorrelationId();

        assertEquals(fromYamlCorrelation.getRaw(), fromJsonCorrelation.getRaw());
        assertEquals(fromYamlCorrelation.getSource(), fromJsonCorrelation.getSource());
        assertEquals(fromYamlCorrelation.getPointer(), fromJsonCorrelation.getPointer());
    }

    @Test
    public void testVersion2IsRejected() {

        //2.x nests its operations inside channels and has no reply at all: a different model
        AsyncApiParsingException e = assertThrows(AsyncApiParsingException.class, () -> parse(
                "asyncapi: 2.6.0\n"
                        + "info:\n"
                        + "  title: The previous major version\n"
                        + "  version: 1.0.0\n"
                        + "channels:\n"
                        + "  user/signup:\n"
                        + "    publish:\n"
                        + "      message:\n"
                        + "        payload:\n"
                        + "          type: object\n"));

        assertTrue(e.getMessage().contains("2.6.0"), e.getMessage());
        assertTrue(e.getMessage().contains("3.x"), e.getMessage());
    }

    @Test
    public void testUnquotedNumericVersionIsStillReadAsText() {

        //YAML would make '3.0' a number, and free-text fields elsewhere likewise
        AsyncApiDocument document = parse(
                "asyncapi: 3.0\n"
                        + "info:\n"
                        + "  title: Numeric looking\n"
                        + "  version: 1.0.0\n");

        assertEquals("3.0", document.getVersion());
    }

    @Test
    public void testOpenApiDocumentIsRejected() {

        AsyncApiParsingException e = assertThrows(AsyncApiParsingException.class, () -> parse(
                "openapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: An OpenAPI document, handed over by mistake\n"
                        + "  version: 1.0.0\n"
                        + "paths: {}\n"));

        //the message has to say what to do about it, not just that it failed
        assertTrue(e.getMessage().contains("OpenAPI"), e.getMessage());
    }

    @Test
    public void testUnreadableDocumentIsRejected() {

        AsyncApiParsingException e = assertThrows(
                AsyncApiParsingException.class,
                () -> parse("asyncapi: 3.0.0\n  badly: [indented"));

        assertTrue(e.getMessage().contains("Failed to parse"), e.getMessage());
    }

    @Test
    public void testDocumentThatIsNotAnObjectIsRejected() {

        AsyncApiParsingException e = assertThrows(
                AsyncApiParsingException.class,
                () -> parse("- just\n- a list"));

        assertTrue(e.getMessage().contains("not a JSON/YAML object"), e.getMessage());
    }

    @Test
    public void testDocumentDeclaringNoMessages() {

        //valid, just empty. Nothing to report and nothing to raise
        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Empty\n"
                        + "  version: 1.0.0\n");

        assertTrue(document.getMessages().isEmpty());
        assertTrue(document.getComponentSchemas().isEmpty());
        assertTrue(document.getWarnings().isEmpty());
        assertTrue(document.getOperations().isEmpty());
    }

    // ------------------------------------------------------------------ correlation

    @Test
    public void testCorrelationInHeader() {

        AsyncApiMessage message =
                load("/asyncapi/artificial/messages.yaml").getMessages().get("signupRequest");

        AsyncApiCorrelationId correlation = message.getCorrelationId();
        assertNotNull(correlation);
        assertEquals(AsyncApiCorrelationId.Source.HEADER, correlation.getSource());
        assertEquals("/correlationId", correlation.getPointer());
        assertEquals("correlationId", correlation.getFieldName());
    }

    @Test
    public void testCorrelationInPayload() {

        AsyncApiMessage message =
                load("/asyncapi/artificial/messages.yaml").getMessages().get("signupReply");

        AsyncApiCorrelationId correlation = message.getCorrelationId();
        assertNotNull(correlation);
        //a transport with no headers, such as a socket, can only carry the id in the payload
        assertEquals(AsyncApiCorrelationId.Source.PAYLOAD, correlation.getSource());
        assertEquals("/request_id", correlation.getPointer());
    }

    @Test
    public void testCorrelationExpressionsThatCannotBeUsed() {

        //not one of the two runtime expressions the specification defines
        assertNull(AsyncApiCorrelationId.parse("somewhere/else"));
        assertNull(AsyncApiCorrelationId.parse("$message.header#"));
        assertNull(AsyncApiCorrelationId.parse("$message.header#noSlash"));

        //a pointer more than one level deep has no single field name
        AsyncApiCorrelationId nested = AsyncApiCorrelationId.parse("$message.payload#/meta/id");
        assertEquals("/meta/id", nested.getPointer());
        assertNull(nested.getFieldName());

        //JSON Pointer escaping is undone, so a field whose name contains a slash still reads
        assertEquals("a/b", AsyncApiCorrelationId.parse("$message.header#/a~1b").getFieldName());

        //surrounding space is not a reason to reject it
        assertEquals(
                AsyncApiCorrelationId.Source.HEADER,
                AsyncApiCorrelationId.parse("  $message.header#/x  ").getSource());
    }

    @Test
    public void testUnsupportedCorrelationExpressionIsReported() {

        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Correlated the wrong way\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    m:\n"
                        + "      correlationId:\n"
                        + "        location: 'somewhere/else'\n"
                        + "      payload:\n"
                        + "        type: object\n");

        //the message is still perfectly usable, it just cannot be paired with a reply
        assertNull(document.getMessages().get("m").getCorrelationId());
        assertTrue(warns(document, "somewhere/else"), document.getWarnings().toString());
    }

    // ------------------------------------------------------------------ traits

    @Test
    public void testMessageTraitsAreMerged() {

        AsyncApiMessage message =
                load("/asyncapi/artificial/messages.yaml").getMessages().get("signupRequest");

        //correlation and headers come from the trait, and are indistinguishable from its own
        assertEquals(AsyncApiCorrelationId.Source.HEADER, message.getCorrelationId().getSource());
        assertTrue(message.getHeaders().get("properties").has("correlationId"));
    }

    @Test
    public void testTraitsThatCannotBeUsedAreReported() {

        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Broken traits\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messageTraits:\n"
                        + "    first:\n"
                        + "      title: from the first trait\n"
                        + "      summary: overridden by the second\n"
                        + "    second:\n"
                        + "      summary: from the second trait\n"
                        + "  messages:\n"
                        + "    merged:\n"
                        + "      traits:\n"
                        + "        - $ref: '#/components/messageTraits/first'\n"
                        + "        - $ref: '#/components/messageTraits/second'\n"
                        + "      description: what the message states itself\n"
                        + "    broken:\n"
                        + "      traits:\n"
                        + "        - 'not an object at all'\n"
                        + "        - $ref: '#/components/messageTraits/absent'\n"
                        + "      payload:\n"
                        + "        type: object\n");

        AsyncApiMessage merged = document.getMessages().get("merged");
        //traits are merged in declaration order, so the later one wins where they overlap
        assertEquals("from the first trait", merged.getTitle());
        assertEquals("from the second trait", merged.getSummary());
        assertEquals("what the message states itself", merged.getDescription());

        //an unusable trait costs only the trait: the message survives
        assertTrue(document.getMessages().containsKey("broken"));
        assertTrue(warns(document, "is not an object"), document.getWarnings().toString());
        //the reference that failed is named, which is what makes the warning actionable
        assertTrue(warns(document, "#/components/messageTraits/absent"),
                document.getWarnings().toString());
    }

    // ------------------------------------------------------------------ schema formats

    @Test
    public void testAvroDeclaredOnTheComponentSchema() {

        AsyncApiDocument document = load("/asyncapi/artificial/message-schema-formats.yaml");

        //Avro is declared on the schema, not on the payload, so the drop has to propagate
        assertFalse(document.getComponentSchemas().containsKey("customer-value"));
        assertFalse(document.getMessages().containsKey("customer"));

        //while a JSON Schema in the same document is unaffected
        assertTrue(document.getComponentSchemas().containsKey("order-value"));
        assertTrue(document.getMessages().containsKey("order"));

        assertTrue(warns(document, "avro"), document.getWarnings().toString());
    }

    @Test
    public void testMultiFormatWrapperIsUnwrapped() {

        AsyncApiMessage message =
                load("/asyncapi/artificial/message-schema-formats.yaml").getMessages().get("wrapped");

        //a dialect that is JSON Schema keeps the schema one level down; it must be lifted out
        assertNull(message.getPayload().get("schemaFormat"));
        assertEquals("object", message.getPayload().get("type").asText());
    }

    @Test
    public void testAContentTypeWrittenWhereASchemaFormatBelongs() {

        AsyncApiDocument document = load("/asyncapi/artificial/message-schema-formats.yaml");

        /*
            "application/json" and "application/yaml" are content types, not schema formats, so
            the specification does not list them here. Documents written by hand put them here
            anyway, and the only reading is that the schema is JSON or YAML -- which for a schema
            means JSON Schema. Dropping such a message would lose one that is perfectly readable.
         */
        for (String id : new String[]{"contentTypeAsFormat", "yamlContentTypeAsFormat"}) {
            AsyncApiMessage message = document.getMessages().get(id);
            assertNotNull(message, id);
            //the wrapper is unwrapped, exactly as for a format the specification does define
            assertNull(message.getPayload().get("schemaFormat"), id);
            assertEquals("object", message.getPayload().get("type").asText(), id);
        }
    }

    // ------------------------------------------------------------------ what a payload can reach

    @Test
    public void testPayloadWhoseSchemaReachesAMissingOneIsDropped() {

        AsyncApiDocument document = load("/asyncapi/artificial/message-schema-references.yaml");

        /*
            The payload resolves and so does the schema it names -- it is the schema *that one*
            reaches which is missing. Only following the chain finds it, and it has to be found:
            whatever consumes this payload would fail on a reference it cannot resolve.
         */
        assertFalse(document.getMessages().containsKey("nested"));
        assertTrue(warns(document, "NotDeclared"), document.getWarnings().toString());
    }

    @Test
    public void testPayloadInAnotherSchemaDialectIsDropped() {

        AsyncApiDocument document = load("/asyncapi/artificial/message-schema-references.yaml");

        //'#/definitions/...' is draft-04's layout, and nothing in this document answers it
        assertFalse(document.getMessages().containsKey("otherDialect"));
        assertTrue(warns(document, "#/definitions/Foo"), document.getWarnings().toString());
    }

    @Test
    public void testPayloadPointingIntoAnotherDocumentIsDropped() {

        AsyncApiDocument document = load("/asyncapi/artificial/message-schema-references.yaml");

        //documents split across files are not read yet, so such a payload cannot be built from
        assertFalse(document.getMessages().containsKey("otherDocument"));
        assertTrue(warns(document, "shared.yaml"), document.getWarnings().toString());
    }

    @Test
    public void testPointerDeeperThanASchemaIsAccepted() {

        AsyncApiDocument document = load("/asyncapi/artificial/message-schema-references.yaml");

        //what matters is that the schema it points into is present
        assertTrue(document.getMessages().containsKey("deepPointer"));
        assertTrue(document.getMessages().containsKey("fine"));
    }

    @Test
    public void testPayloadThatIsNotAnObjectIsDroppedWithAWarning() {

        /*
            `true` is a valid JSON Schema meaning "any payload at all", but it describes no shape
            to build from, so the message goes. What matters here is that it is reported: a
            message that vanishes with nothing in the warnings cannot be traced back to the line
            of the document that caused it.
         */
        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Boolean payload\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    anything:\n"
                        + "      payload: true\n");

        assertFalse(document.getMessages().containsKey("anything"));
        assertTrue(warns(document, "anything", "boolean true"), document.getWarnings().toString());
    }

    @Test
    public void testHeadersThatAreNotAnObjectCostOnlyTheHeaders() {

        //a broken headers declaration is reported too, but the message itself is still usable
        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Broken headers\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    usable:\n"
                        + "      headers: []\n"
                        + "      payload:\n"
                        + "        type: object\n");

        assertTrue(document.getMessages().containsKey("usable"));
        assertNull(document.getMessages().get("usable").getHeaders());
        assertTrue(warns(document, "headers of message 'usable'", "an array"),
                document.getWarnings().toString());
    }

    @Test
    public void testAMessageDeclaringNoPayloadIsNotReportedAsBroken() {

        //absent is not the same as unreadable, and must stay silent
        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: No payload\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    empty:\n"
                        + "      title: nothing to say\n");

        assertTrue(document.getMessages().containsKey("empty"));
        assertTrue(document.getWarnings().isEmpty(), document.getWarnings().toString());
    }

    @Test
    public void testAnUnresolvableTraitIsReportedOnlyOnce() {

        /*
            The reference that failed is named by the code that followed it. Reporting it a
            second time, in vaguer terms, only makes the warnings harder to read.
         */
        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Missing trait\n"
                        + "  version: 1.0.0\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    withTrait:\n"
                        + "      traits:\n"
                        + "        - $ref: '#/components/messageTraits/notThere'\n"
                        + "      payload:\n"
                        + "        type: object\n");

        assertEquals(1, document.getWarnings().size(), document.getWarnings().toString());
        assertTrue(warns(document, "#/components/messageTraits/notThere"),
                document.getWarnings().toString());
    }

    @Test
    public void testBrokenHeadersCostOnlyTheHeaders() {

        AsyncApiMessage message = load("/asyncapi/artificial/message-schema-references.yaml")
                .getMessages().get("badHeaders");

        //the message is still perfectly usable, so it is kept without its headers
        assertNotNull(message.getPayload());
        assertNull(message.getHeaders());
    }

    // ------------------------------------------------------------------ cycles

    @Test
    public void testReferenceCyclesDoNotTakeTheDocumentDown() {

        //without a guard each of these recurses until the stack gives out, killing the run
        AsyncApiDocument document = load("/asyncapi/artificial/reference-cycles.yaml");

        //the circular messages and correlation ids are dropped, each explained
        assertFalse(document.getMessages().containsKey("ping"));
        assertFalse(document.getMessages().containsKey("itself"));
        assertTrue(warns(document, "cycle"), document.getWarnings().toString());

        //a correlationId that only points at itself leaves the message without one
        assertNull(document.getMessages().get("request").getCorrelationId());
    }

    @Test
    public void testSchemasMayReferToThemselves() {

        //a self-referring schema is a tree, and perfectly legitimate: it must not be confused
        //with a broken reference, nor send the reachability check round for ever
        AsyncApiDocument document = load("/asyncapi/artificial/reference-cycles.yaml");

        assertTrue(document.getMessages().containsKey("request"));
        assertTrue(document.getComponentSchemas().containsKey("Node"));
    }

    // ------------------------------------------------------------------ channels and operations

    @Test
    public void testInlineMessagesArePromoted() {

        AsyncApiDocument document = load("/asyncapi/artificial/inline-messages.yaml");

        //no components.messages at all: everything was written inside its channel
        assertEquals(2, document.getMessages().size());
        assertTrue(document.getMessages().containsKey("signup.request"));
        assertTrue(document.getMessages().containsKey("signupReply.ok"));

        AsyncApiChannel channel = document.getChannels().get("signup");
        assertEquals("user/signup", channel.getAddress());
        assertEquals(setOf("request"), channel.getMessageKeys().keySet());
        assertEquals("signup.request", channel.getMessageKeys().get("request"));

        AsyncApiMessage message = document.getMessages().get("signup.request");
        assertEquals("SignupRequest", message.getName());
        //assert the content, so that swapping payload and headers would be caught
        assertTrue(message.getPayload().get("properties").has("email"));
        assertTrue(message.getHeaders().get("properties").has("correlationId"));
    }

    @Test
    public void testInlineChannelMessageThatIsNotAnObjectIsDroppedWithAWarning() {

        /*
            A message written inside a channel is dropped like any other when its payload is not
            a schema that can be read. The channel is the path where that used to happen in
            silence: nothing is registered under the local key, and unlike a message referenced
            by '$ref' there is no second warning about the channel to hint at what went missing.
         */
        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: An inline message that cannot be read\n"
                        + "  version: 1.0.0\n"
                        + "channels:\n"
                        + "  c:\n"
                        + "    address: a\n"
                        + "    messages:\n"
                        + "      broken:\n"
                        + "        payload: not a schema\n"
                        + "      fine:\n"
                        + "        payload:\n"
                        + "          type: object\n");

        AsyncApiChannel channel = document.getChannels().get("c");

        //the one that could be read is still there, so the channel is not lost with it
        assertEquals(setOf("fine"), channel.getMessageKeys().keySet());
        assertTrue(warns(document, "not a schema"), document.getWarnings().toString());
    }

    @Test
    public void testChannelWithoutAddressAndDynamicReplyAddress() {

        AsyncApiDocument document = load("/asyncapi/artificial/inline-messages.yaml");

        //an explicit 'address: null' means the address is only known at run time
        assertNull(document.getChannels().get("signupReply").getAddress());

        AsyncApiReply reply = document.getOperations().get("onSignup").getReply();
        assertNotNull(reply);
        assertEquals("signupReply", reply.getChannelName());
        assertEquals("$message.header#/replyTo", reply.getAddressLocation());
        //no explicit message selection: everything the reply channel carries
        assertEquals(Arrays.asList("signupReply.ok"), reply.getMessageIds());
    }

    @Test
    public void testOperationSelectsSubsetOfChannelMessages() {

        AsyncApiDocument document = load("/asyncapi/artificial/websocket-reply.yaml");

        //one duplex channel carrying five different messages
        AsyncApiChannel channel = document.getChannels().get("vsi");
        assertEquals(5, channel.getMessageIds().size());

        AsyncApiOperation operation = document.getOperations().get("recv_list_legs");
        assertEquals(Arrays.asList("listLegs"), operation.getMessageIds());

        AsyncApiReply reply = operation.getReply();
        //the reply comes back on the very same channel: there is only one socket
        assertEquals("vsi", reply.getChannelName());
        assertEquals(Arrays.asList("listLegsResult", "error"), reply.getMessageIds());
    }

    @Test
    public void testChannelLocalMessageKeysDifferFromMessageIds() {

        AsyncApiChannel channel =
                load("/asyncapi/artificial/websocket-reply.yaml").getChannels().get("vsi");

        //the key a $ref uses is the channel's own, not the component id
        assertEquals("listLegsResult", channel.getMessageKeys().get("list_legs.result"));
        assertEquals("error", channel.getMessageKeys().get("error"));
    }

    @Test
    public void testNarrowedSelectionIsNotWidenedWhenItsMessageIsSkipped() {

        AsyncApiDocument document = load("/asyncapi/artificial/narrowed-selection.yaml");

        //the Avro message could not be read, so the channel is left with only the other one
        assertEquals(Arrays.asList("usable"), document.getChannels().get("events").getMessageIds());

        //an operation that asked for the skipped message gets nothing, rather than the other one
        assertTrue(document.getOperations().get("onUnreadable").getMessageIds().isEmpty());
        assertEquals(Arrays.asList("usable"), document.getOperations().get("onUsable").getMessageIds());

        //while one that asked for nothing in particular gets what is left
        assertEquals(Arrays.asList("usable"), document.getOperations().get("onAnything").getMessageIds());

        assertTrue(warns(document, "not available"), document.getWarnings().toString());
    }

    @Test
    public void testAChannelMayOverrideWhatItReferences() {

        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: A channel adding to a shared message\n"
                        + "  version: 1.0.0\n"
                        + "channels:\n"
                        + "  c:\n"
                        + "    address: a\n"
                        + "    messages:\n"
                        + "      m:\n"
                        + "        $ref: '#/components/messages/shared'\n"
                        + "        title: only on this channel\n"
                        + "operations:\n"
                        + "  o:\n"
                        + "    action: receive\n"
                        + "    channel:\n"
                        + "      $ref: '#/channels/c'\n"
                        + "components:\n"
                        + "  messages:\n"
                        + "    shared:\n"
                        + "      name: Shared\n"
                        + "      payload:\n"
                        + "        type: object\n");

        /*
            The override makes it a variant belonging to this channel, registered under its own
            id. The shared definition must be left alone, or every other channel carrying the
            same message would silently inherit something meant for this one.
         */
        assertEquals(Arrays.asList("c.m"), document.getOperations().get("o").getMessageIds());
        assertEquals("only on this channel", document.getMessages().get("c.m").getTitle());
        assertEquals("Shared", document.getMessages().get("c.m").getName());
        assertNull(document.getMessages().get("shared").getTitle());
    }

    @Test
    public void testSendAndReceiveKeepTheirDirection() {

        AsyncApiDocument document = load("/asyncapi/artificial/broken-parts.yaml");

        /*
            The polarity matters and is easy to invert: 'receive' is what the service consumes,
            so it is what a tester would publish to, and 'send' is what it emits.
         */
        assertEquals(AsyncApiOperation.Action.RECEIVE, document.getOperations().get("works").getAction());
        assertEquals(
                AsyncApiOperation.Action.SEND,
                document.getOperations().get("badCorrelationTarget").getAction());
    }

    @Test
    public void testOperationTraitsAreMerged() {

        AsyncApiDocument document = parse(
                "asyncapi: 3.0.0\n"
                        + "info:\n"
                        + "  title: Boilerplate factored out of the operations\n"
                        + "  version: 1.0.0\n"
                        + "channels:\n"
                        + "  c:\n"
                        + "    address: a\n"
                        + "    messages:\n"
                        + "      m:\n"
                        + "        payload:\n"
                        + "          type: object\n"
                        + "operations:\n"
                        + "  o:\n"
                        + "    action: receive\n"
                        + "    channel:\n"
                        + "      $ref: '#/channels/c'\n"
                        + "    traits:\n"
                        + "      - $ref: '#/components/operationTraits/documented'\n"
                        + "    summary: what the operation states itself\n"
                        + "components:\n"
                        + "  operationTraits:\n"
                        + "    documented:\n"
                        + "      summary: overridden by the operation\n"
                        + "      description: from the trait\n");

        AsyncApiOperation operation = document.getOperations().get("o");
        assertEquals("what the operation states itself", operation.getSummary());
        assertEquals("from the trait", operation.getDescription());
    }

    // ------------------------------------------------------------------ degrading gracefully

    @Test
    public void testBrokenPartsAreSkippedAndTheRestSurvives() {

        AsyncApiDocument document = load("/asyncapi/artificial/broken-parts.yaml");

        //only the two well-formed operations are kept
        assertEquals(setOf("works", "badCorrelationTarget"), document.getOperations().keySet());

        assertTrue(warns(document, "noAction"), document.getWarnings().toString());
        assertTrue(warns(document, "wrongAction"), document.getWarnings().toString());
        assertTrue(warns(document, "missingChannel"), document.getWarnings().toString());
        assertTrue(warns(document, "doesNotExist"), document.getWarnings().toString());

        //the dangling message reference costs only that one message
        assertEquals(Arrays.asList("request"), document.getChannels().get("good").getMessageIds());
    }

    // ------------------------------------------------------------------ the model's read API

    @Test
    public void testResolvingOperationsToTheirChannelAndMessages() {

        AsyncApiDocument document = load("/asyncapi/artificial/websocket-reply.yaml");
        AsyncApiOperation operation = document.getOperations().get("recv_list_legs");

        assertEquals("vsi", document.channelOf(operation).getName());
        assertEquals("vsi", document.replyChannelOf(operation).getName());
        assertEquals(Arrays.asList("ListLegs"), namesOf(document.messagesOf(operation)));
        assertEquals(
                Arrays.asList("ListLegsResult", "Error"),
                namesOf(document.replyMessagesOf(operation)));
    }

    @Test
    public void testResolvingAnOperationThatDeclaresNoReply() {

        AsyncApiDocument document = load("/asyncapi/artificial/broken-parts.yaml");
        AsyncApiOperation operation = document.getOperations().get("works");

        assertNull(operation.getReply());
        assertNull(document.replyChannelOf(operation));
        assertTrue(document.replyMessagesOf(operation).isEmpty());
    }

}
