package com.webfuzzing.asyncapi.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.webfuzzing.asyncapi.mapper.AsyncApiMapper;
import com.webfuzzing.asyncapi.models.AsyncApiChannel;
import com.webfuzzing.asyncapi.models.AsyncApiChannelBindings;
import com.webfuzzing.asyncapi.models.AsyncApiCorrelationId;
import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.models.AsyncApiMessage;
import com.webfuzzing.asyncapi.models.AsyncApiOperation;
import com.webfuzzing.asyncapi.models.AsyncApiReply;
import com.webfuzzing.asyncapi.models.AsyncApiSecurityScheme;
import com.webfuzzing.asyncapi.models.AsyncApiServer;
import com.webfuzzing.asyncapi.models.AsyncApiServerVariable;
import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.resolver.AsyncApiDocumentFetcher;
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver;
import com.webfuzzing.asyncapi.resolver.RefLocations;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns the text of an AsyncAPI 3.x document into an {@link AsyncApiDocument}.
 *
 * This is a purpose-built parser rather than a complete implementation of the specification.
 * It reads what a client acts on and ignores the rest; a keyword it does not know is simply not
 * read, and is never an error.
 *
 * Failures are graded. A document that cannot be read at all, is of the wrong version, or is
 * not an AsyncAPI document raises an {@link AsyncApiParsingException}. Anything narrower -- one
 * broken message, one unresolvable reference, one payload in a format that is not JSON Schema
 * -- is recorded in {@link AsyncApiDocument#getWarnings()} and costs only the element it
 * affects.
 */
public class AsyncApiParser {

    /**
     * The AsyncAPI keywords this parser reads, spelled as the specification spells them. Kept
     * together so that a field name is written once, and so that what the parser understands
     * can be seen in one place.
     */
    private static final class Keyword {

        static final String REF = "$ref";

        static final String ACTION = "action";
        static final String ADDRESS = "address";
        static final String AMQP = "amqp";
        static final String ASYNCAPI = "asyncapi";
        static final String BEARER_FORMAT = "bearerFormat";
        static final String BINDINGS = "bindings";
        static final String CHANNEL = "channel";
        static final String CHANNELS = "channels";
        static final String COMPONENTS = "components";
        static final String CONTENT_TYPE = "contentType";
        static final String CORRELATION_ID = "correlationId";
        static final String DEFAULT = "default";
        static final String DEFAULT_CONTENT_TYPE = "defaultContentType";
        static final String DESCRIPTION = "description";
        static final String ENUM = "enum";
        static final String EXAMPLES = "examples";
        static final String EXCHANGE = "exchange";
        static final String HEADERS = "headers";
        static final String HOST = "host";
        static final String IN = "in";
        static final String IS = "is";
        static final String KEY = "key";
        static final String LOCATION = "location";
        static final String MESSAGES = "messages";
        static final String MESSAGE_TRAITS = "messageTraits";
        static final String METHOD = "method";
        static final String NAME = "name";
        static final String OPERATIONS = "operations";
        static final String OPERATION_TRAITS = "operationTraits";
        static final String PARAMETERS = "parameters";
        static final String PATHNAME = "pathname";
        static final String PAYLOAD = "payload";
        static final String PROTOCOL = "protocol";
        static final String PROTOCOL_VERSION = "protocolVersion";
        static final String QUEUE = "queue";
        static final String RECEIVE = "receive";
        static final String REPLY = "reply";
        static final String SCHEMA = "schema";
        static final String SCHEMA_FORMAT = "schemaFormat";
        static final String SCHEMAS = "schemas";
        static final String SCHEME = "scheme";
        static final String SECURITY = "security";
        static final String SECURITY_SCHEMES = "securitySchemes";
        static final String SEND = "send";
        static final String SERVERS = "servers";
        static final String SUMMARY = "summary";
        static final String TITLE = "title";
        static final String TOPIC = "topic";
        static final String TRAITS = "traits";
        static final String TYPE = "type";
        static final String VARIABLES = "variables";
        static final String WS = "ws";

        private Keyword() {
        }
    }

    private static final String POINTER_ROOT = RefLocations.FRAGMENT_SEPARATOR + RefLocations.PATH_SEPARATOR;

    private static final String CHANNEL_REF_PREFIX = POINTER_ROOT + Keyword.CHANNELS + RefLocations.PATH_SEPARATOR;

    private static final String SERVER_REF_PREFIX = POINTER_ROOT + Keyword.SERVERS + RefLocations.PATH_SEPARATOR;

    private static final String COMPONENT_REF_PREFIX = POINTER_ROOT + Keyword.COMPONENTS + RefLocations.PATH_SEPARATOR;

    private static final String MESSAGE_REF_PREFIX = COMPONENT_REF_PREFIX + Keyword.MESSAGES + RefLocations.PATH_SEPARATOR;

    private static final String SECURITY_REF_PREFIX =
            COMPONENT_REF_PREFIX + Keyword.SECURITY_SCHEMES + RefLocations.PATH_SEPARATOR;

    /**
     * Schema formats that are JSON Schema by another name, and so can be read here. Compared as
     * prefixes, since the format string carries a version suffix.
     *
     * The first four are what the specification defines. The last two are not: "application/json"
     * and "application/yaml" are content types rather than schema formats, and belong in a
     * message's `contentType`. They are accepted anyway because hand-written documents do put
     * them here, and the only thing they can be taken to mean is that the schema is written in
     * JSON or YAML -- which, for a schema, is JSON Schema. Rejecting them would drop a message
     * that is perfectly readable.
     *
     * Anything else -- Avro and Protobuf being the ones that actually turn up -- describes a
     * payload in a language this parser does not speak, and the message is dropped.
     */
    private static final List<String> JSON_SCHEMA_FORMATS = Arrays.asList(
            "application/vnd.aai.asyncapi",
            "application/schema+json",
            "application/schema+yaml",
            "application/vnd.oai.openapi",
            "application/json",
            "application/yaml"
    );

    private AsyncApiParser() {
    }

    /**
     * Parse {@code schemaText}, reaching for any document it refers to with {@code fetch}.
     */
    public static AsyncApiDocument parse(
            String schemaText,
            DocumentLocation location,
            AsyncApiDocumentFetcher fetch) {

        JsonNode root;
        try {
            root = AsyncApiMapper.readTree(schemaText);
        } catch (Exception e) {
            throw new AsyncApiParsingException("Failed to parse the AsyncAPI document: " + e.getMessage(), e);
        }

        if (root == null || !root.isObject()) {
            throw new AsyncApiParsingException("The AsyncAPI document is not a JSON/YAML object");
        }

        String version = scalarOf(root.get(Keyword.ASYNCAPI));

        if (version == null) {
            throw new AsyncApiParsingException(
                    "The document has no 'asyncapi' field, so it is not an AsyncAPI document."
                            + " If this is an OpenAPI schema, it describes a different kind of API"
                            + " and has to be read by an OpenAPI parser instead.");
        }

        if (!version.startsWith("3.")) {
            throw new AsyncApiParsingException(
                    "AsyncAPI version '" + version + "' is not supported. Only 3.x is handled at the"
                            + " moment; in particular 2.x has no first-class reply, and is not parsed yet.");
        }

        List<String> warnings = new ArrayList<>();

        AsyncApiRefResolver.inlineExternalDocuments(
                (ObjectNode) root, location, warnings, fetch, AsyncApiMapper::readTree);

        String defaultContentType = scalarOf(root.get(Keyword.DEFAULT_CONTENT_TYPE));
        if (defaultContentType == null) {
            defaultContentType = AsyncApiDocument.DEFAULT_CONTENT_TYPE;
        }

        //security schemes are a mutable map, as schemes can also be declared inline where used
        Map<String, AsyncApiSecurityScheme> securitySchemes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : componentsOf(root, Keyword.SECURITY_SCHEMES).entrySet()) {
            AsyncApiSecurityScheme scheme = parseSecurityScheme(entry.getKey(), entry.getValue(), root);
            if (scheme == null) {
                //same as for one written inline: a scheme with no 'type' says nothing usable
                warnings.add(
                        "The security scheme '" + entry.getKey() + "' declares no 'type'."
                                + " It is ignored.");
            } else {
                securitySchemes.put(entry.getKey(), scheme);
            }
        }

        Map<String, JsonNode> componentSchemas = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : componentsOf(root, Keyword.SCHEMAS).entrySet()) {
            JsonNode schema = schemaOf(entry.getValue(), "the component schema '" + entry.getKey() + "'", warnings);
            if (schema != null) {
                componentSchemas.put(entry.getKey(), schema);
            }
        }

        //messages first: channels refer to them, and inline ones are added to the same map
        Map<String, AsyncApiMessage> messages = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : componentsOf(root, Keyword.MESSAGES).entrySet()) {
            AsyncApiMessage message = parseMessage(
                    entry.getKey(), entry.getValue(), root, defaultContentType, componentSchemas, warnings);
            if (message != null) {
                messages.put(entry.getKey(), message);
            }
        }

        JsonNode channelsNode = root.get(Keyword.CHANNELS);
        Map<String, AsyncApiChannel> channels = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : objectFieldsOf(channelsNode).entrySet()) {
            channels.put(entry.getKey(), parseChannel(
                    entry.getKey(), entry.getValue(), root, defaultContentType,
                    componentSchemas, messages, warnings));
        }

        JsonNode operationsNode = root.get(Keyword.OPERATIONS);
        Map<String, AsyncApiOperation> operations = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : objectFieldsOf(operationsNode).entrySet()) {
            AsyncApiOperation operation = parseOperation(
                    entry.getKey(), entry.getValue(), root, channels, messages, securitySchemes, warnings);
            if (operation != null) {
                operations.put(entry.getKey(), operation);
            }
        }

        JsonNode serversNode = root.get(Keyword.SERVERS);
        Map<String, AsyncApiServer> servers = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : objectFieldsOf(serversNode).entrySet()) {
            AsyncApiServer server =
                    parseServer(entry.getKey(), entry.getValue(), root, securitySchemes, warnings);
            if (server != null) {
                servers.put(entry.getKey(), server);
            }
        }

        return AsyncApiDocument.builder(schemaText, location, version)
                .defaultContentType(defaultContentType)
                .servers(servers)
                .channels(channels)
                .operations(operations)
                .messages(messages)
                .componentSchemas(componentSchemas)
                .securitySchemes(securitySchemes)
                .warnings(warnings)
                .build();
    }

    // ------------------------------------------------------------------ messages

    private static AsyncApiMessage parseMessage(
            String id,
            JsonNode rawNode,
            JsonNode root,
            String defaultContentType,
            Map<String, JsonNode> componentSchemas,
            List<String> warnings) {

        JsonNode declared = dereference(rawNode, root, warnings);

        if (declared == null) {
            return null;
        }

        JsonNode node = applyTraits(declared, root, Keyword.MESSAGE_TRAITS, warnings);

        JsonNode payload = schemaOf(node.get(Keyword.PAYLOAD), "message '" + id + "'", warnings);

        if (payload != null && reportUnfollowable(
                payload, componentSchemas, "message '" + id + "'", "The message is ignored.", warnings)) {
            payload = null;
        }

        if (payload == null && node.has(Keyword.PAYLOAD)) {
            //nothing can be built from a payload that cannot be read, so the message goes too
            return null;
        }

        //broken headers cost only the headers: the message itself is still usable
        JsonNode headers = schemaOf(node.get(Keyword.HEADERS), "the headers of message '" + id + "'", warnings);

        if (headers != null && reportUnfollowable(
                headers, componentSchemas, "the headers of message '" + id + "'",
                "The headers are ignored.", warnings)) {
            headers = null;
        }

        Map<String, JsonNode> bindings = dereferencedFields(node.get(Keyword.BINDINGS), root, warnings);
        JsonNode kafka = bindings.get(AsyncApiChannel.KAFKA);

        return AsyncApiMessage.builder(id)
                .name(scalarOr(node.get(Keyword.NAME), id))
                .contentType(scalarOr(node.get(Keyword.CONTENT_TYPE), defaultContentType))
                .payload(payload)
                .headers(headers)
                .correlationId(parseCorrelationId(node.get(Keyword.CORRELATION_ID), root, id, warnings))
                .kafkaKey(kafka == null ? null : kafka.get(Keyword.KEY))
                .bindings(bindings)
                .examples(objectsOf(node.get(Keyword.EXAMPLES)))
                .title(scalarOf(node.get(Keyword.TITLE)))
                .summary(scalarOf(node.get(Keyword.SUMMARY)))
                .description(scalarOf(node.get(Keyword.DESCRIPTION)))
                .build();
    }

    /**
     * Read a schema declaration as JSON Schema.
     *
     * The specification allows a schema to be wrapped in a "multi format" object stating the
     * language it is written in. When that language is not JSON Schema -- Avro, typically --
     * there is nothing useful to be done with it here, so null is returned and the caller drops
     * whatever depended on it.
     */
    private static JsonNode schemaOf(JsonNode node, String owner, List<String> warnings) {

        if (node == null) {
            //nothing was declared, which is not a problem and has nothing to report
            return null;
        }

        if (node.isNull() || !node.isObject()) {
            /*
                Something was declared, but not as an object. A bare `true` or `false` is in fact
                a valid JSON Schema meaning "anything" and "nothing", and an explicit null is
                written by some generators; none of them describe a shape that genes can be built
                from. They cost whatever declared them, so they have to be reported -- silently
                dropping a message is the one outcome a user cannot trace back to their document.
             */
            warnings.add(
                    "The schema of " + owner + " is written as " + describe(node) + " rather than as a"
                            + " JSON Schema object, so there is no shape to read from it. It is ignored.");
            return null;
        }

        String format = scalarOf(node.get(Keyword.SCHEMA_FORMAT));

        if (format == null) {
            return node;
        }

        for (String known : JSON_SCHEMA_FORMATS) {
            if (format.startsWith(known)) {
                //a multi-format declaration keeps the schema itself one level down
                JsonNode schema = node.get(Keyword.SCHEMA);
                return schema == null ? node : schema;
            }
        }

        warnings.add(
                "The schema of " + owner + " is written in '" + format + "', which is not JSON Schema."
                        + " It is ignored.");

        return null;
    }

    /**
     * The first reference under {@code start} that leads nowhere, or null when all of them lead
     * somewhere.
     *
     * References inside a schema are deliberately left unresolved for a later step to follow,
     * which only works if every one of them can in fact be followed: it must be local, of the
     * {@code #/components/schemas/...} form, and name a schema that is present. So the check has
     * to be transitive -- a payload may reference a perfectly good schema whose own properties
     * reference one that was dropped for being written in Avro, and following that chain is the
     * only way to notice.
     *
     * Schemas may reference each other in a cycle quite legitimately, hence the visited set.
     */
    private static String unfollowableSchemaRef(JsonNode start, Map<String, JsonNode> componentSchemas) {

        Set<String> visited = new LinkedHashSet<>();
        Deque<JsonNode> pending = new ArrayDeque<>();
        pending.add(start);

        while (!pending.isEmpty()) {

            for (String ref : AsyncApiRefResolver.collectRefs(pending.removeFirst())) {

                //anything outside this document cannot be followed later
                if (!AsyncApiRefResolver.isLocal(ref)) {
                    return ref;
                }

                String key = AsyncApiRefResolver.schemaKeyOf(ref);

                if (key == null) {
                    return ref;
                }

                JsonNode target = componentSchemas.get(key);

                if (target == null) {
                    return ref;
                }

                if (visited.add(key)) {
                    pending.add(target);
                }
            }
        }

        return null;
    }

    /**
     * Report a schema whose references cannot all be followed, naming what depends on it.
     */
    private static boolean reportUnfollowable(
            JsonNode schema,
            Map<String, JsonNode> componentSchemas,
            String owner,
            String consequence,
            List<String> warnings) {

        String ref = unfollowableSchemaRef(schema, componentSchemas);

        if (ref == null) {
            return false;
        }

        warnings.add(
                "The schema of " + owner + " refers to '" + ref + "', which is not declared or could"
                        + " not be read. " + consequence);

        return true;
    }

    private static AsyncApiCorrelationId parseCorrelationId(
            JsonNode rawNode,
            JsonNode root,
            String messageId,
            List<String> warnings) {

        if (rawNode == null || rawNode.isNull()) {
            return null;
        }

        JsonNode node = dereference(rawNode, root, warnings);

        if (node == null) {
            return null;
        }

        String location = scalarOf(node.get(Keyword.LOCATION));

        if (location == null) {
            warnings.add(
                    "Message '" + messageId + "' declares a correlationId with no 'location', so there"
                            + " is no way to know where the id travels");
            return null;
        }

        AsyncApiCorrelationId parsed =
                AsyncApiCorrelationId.parse(location, scalarOf(node.get(Keyword.DESCRIPTION)));

        if (parsed == null) {
            warnings.add(
                    "Message '" + messageId + "' declares the correlation id at '" + location + "',"
                            + " which is not a supported runtime expression. It must point inside the"
                            + " message header or payload.");
        }

        return parsed;
    }

    // ------------------------------------------------------------------ servers

    private static AsyncApiServer parseServer(
            String name,
            JsonNode rawNode,
            JsonNode root,
            Map<String, AsyncApiSecurityScheme> securitySchemes,
            List<String> warnings) {

        JsonNode node = dereference(rawNode, root, warnings);

        if (node == null) {
            return null;
        }

        String host = scalarOf(node.get(Keyword.HOST));
        String protocol = scalarOf(node.get(Keyword.PROTOCOL));

        if (host == null || protocol == null) {
            String missing = host == null && protocol == null
                    ? "host and protocol"
                    : (host == null ? "host" : "protocol");
            warnings.add("Server '" + name + "' declares no " + missing + ", and is ignored");
            return null;
        }

        JsonNode variablesNode = node.get(Keyword.VARIABLES);
        Map<String, AsyncApiServerVariable> variables = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : objectFieldsOf(variablesNode).entrySet()) {
            JsonNode variable = entry.getValue();
            variables.put(entry.getKey(), new AsyncApiServerVariable(
                    entry.getKey(),
                    scalarOf(variable.get(Keyword.DEFAULT)),
                    scalarsOf(variable.get(Keyword.ENUM)),
                    scalarOf(variable.get(Keyword.DESCRIPTION))));
        }

        return AsyncApiServer.builder(name, host, protocol)
                .protocolVersion(scalarOf(node.get(Keyword.PROTOCOL_VERSION)))
                .pathname(scalarOf(node.get(Keyword.PATHNAME)))
                .variables(variables)
                .security(parseSecurity(
                        node.get(Keyword.SECURITY), "server '" + name + "'", root, securitySchemes, warnings))
                .build();
    }

    private static AsyncApiSecurityScheme parseSecurityScheme(String name, JsonNode rawNode, JsonNode root) {

        JsonNode node = rawNode;
        String ref = AsyncApiRefResolver.refOf(rawNode);

        if (ref != null) {
            JsonNode resolved = AsyncApiRefResolver.resolveLocal(root, ref);
            if (resolved != null) {
                node = resolved;
            }
        }

        String type = scalarOf(node.get(Keyword.TYPE));

        if (type == null) {
            return null;
        }

        return new AsyncApiSecurityScheme(
                name,
                type.toLowerCase(Locale.ENGLISH),
                scalarOf(node.get(Keyword.IN)),
                scalarOf(node.get(Keyword.SCHEME)),
                scalarOf(node.get(Keyword.BEARER_FORMAT)),
                scalarOf(node.get(Keyword.DESCRIPTION)));
    }

    /**
     * Read a {@code security} array, which may hold either references to declared schemes or
     * schemes written inline. Inline ones are registered under a name derived from where they
     * appear, so that everything is reachable from one map.
     */
    private static List<String> parseSecurity(
            JsonNode node,
            String owner,
            JsonNode root,
            Map<String, AsyncApiSecurityScheme> securitySchemes,
            List<String> warnings) {

        if (node == null || !node.isArray()) {
            return new ArrayList<>();
        }

        List<String> names = new ArrayList<>();

        for (int index = 0; index < node.size(); index++) {

            JsonNode entry = node.get(index);
            String ref = AsyncApiRefResolver.refOf(entry);

            if (ref != null) {

                String key = AsyncApiRefResolver.refKey(ref, SECURITY_REF_PREFIX);

                if (key != null && securitySchemes.containsKey(key)) {
                    names.add(key);
                } else {
                    warnings.add(
                            "The security of " + owner + " refers to '" + ref + "', which is not a"
                                    + " declared security scheme. It is ignored.");
                }

            } else {

                String synthetic = owner + ".security." + index;
                AsyncApiSecurityScheme scheme = parseSecurityScheme(synthetic, entry, root);

                if (scheme == null) {
                    warnings.add(
                            "The security of " + owner + " declares a scheme with no 'type'."
                                    + " It is ignored.");
                } else {
                    securitySchemes.put(synthetic, scheme);
                    names.add(scheme.getName());
                }
            }
        }

        return names;
    }

    // ------------------------------------------------------------------ channels

    private static AsyncApiChannel parseChannel(
            String name,
            JsonNode rawNode,
            JsonNode root,
            String defaultContentType,
            Map<String, JsonNode> componentSchemas,
            Map<String, AsyncApiMessage> messages,
            List<String> warnings) {

        JsonNode dereferenced = dereference(rawNode, root, warnings);
        JsonNode node = dereferenced == null ? rawNode : dereferenced;

        Map<String, String> messageKeys = new LinkedHashMap<>();

        JsonNode messagesNode = node.get(Keyword.MESSAGES);
        for (Map.Entry<String, JsonNode> entry : objectFieldsOf(messagesNode).entrySet()) {
            String id = resolveChannelMessage(
                    name, entry.getKey(), entry.getValue(), root, defaultContentType,
                    componentSchemas, messages, warnings);
            if (id != null) {
                messageKeys.put(entry.getKey(), id);
            }
        }

        List<String> servers = new ArrayList<>();
        for (String ref : refsOf(node.get(Keyword.SERVERS))) {
            String key = AsyncApiRefResolver.refKey(ref, SERVER_REF_PREFIX);
            if (key != null) {
                servers.add(key);
            }
        }

        return AsyncApiChannel.builder(name)
                .address(scalarOf(node.get(Keyword.ADDRESS)))
                .servers(servers)
                .messageKeys(messageKeys)
                .parameters(dereferencedFields(node.get(Keyword.PARAMETERS), root, warnings))
                .bindings(parseChannelBindings(dereferencedFields(node.get(Keyword.BINDINGS), root, warnings)))
                .build();
    }

    /**
     * Work out which message a channel entry stands for, registering it if it was written
     * inline rather than referenced.
     *
     * @return the id under which the message can be found, or null when it could not be read
     */
    private static String resolveChannelMessage(
            String channelName,
            String localKey,
            JsonNode entry,
            JsonNode root,
            String defaultContentType,
            Map<String, JsonNode> componentSchemas,
            Map<String, AsyncApiMessage> messages,
            List<String> warnings) {

        String ref = AsyncApiRefResolver.refOf(entry);
        String id = channelName + "." + localKey;

        if (ref == null) {
            //written inline: promote it, so that it is reachable like any other message
            return register(
                    parseMessage(id, entry, root, defaultContentType, componentSchemas, warnings), messages);
        }

        /*
            A channel may add to what it references. When it does, the result is a variant
            belonging to this channel rather than the shared definition, so it is registered
            separately: another channel carrying the same message must not inherit it.
         */
        boolean overridden = hasFieldsBesidesRef(entry);

        if (!overridden) {

            String componentKey = AsyncApiRefResolver.refKey(ref, MESSAGE_REF_PREFIX);

            if (componentKey != null) {

                if (messages.containsKey(componentKey)) {
                    return componentKey;
                }

                warnings.add(
                        "Channel '" + channelName + "' refers to message '" + componentKey + "', which"
                                + " is not declared (or could not be read). The message is ignored.");
                return null;
            }
        }

        /*
            Either an overridden message, or a pointer somewhere other than the component
            messages. Follow it all the way: the target may itself be an alias, and overlaying
            onto an alias would leave a $ref that later throws the overrides away again.
         */
        JsonNode resolved = dereference(entry, root, warnings);

        if (resolved == null) {
            warnings.add(
                    "Channel '" + channelName + "' has a message '" + localKey + "' that could not be read");
            return null;
        }

        JsonNode definition = overridden ? shallowMerge(resolved, entry) : resolved;

        return register(
                parseMessage(id, definition, root, defaultContentType, componentSchemas, warnings), messages);
    }

    /**
     * Add a message to the map it is reachable from, and give back its id.
     */
    private static String register(AsyncApiMessage message, Map<String, AsyncApiMessage> messages) {

        if (message == null) {
            return null;
        }

        messages.put(message.getId(), message);

        return message.getId();
    }

    private static boolean hasFieldsBesidesRef(JsonNode node) {

        Iterator<String> names = node.fieldNames();

        while (names.hasNext()) {
            if (!Keyword.REF.equals(names.next())) {
                return true;
            }
        }

        return false;
    }

    private static AsyncApiChannelBindings parseChannelBindings(Map<String, JsonNode> raw) {

        JsonNode kafka = raw.get(AsyncApiChannel.KAFKA);
        JsonNode amqp = raw.get(Keyword.AMQP);
        JsonNode ws = raw.get(Keyword.WS);

        return AsyncApiChannelBindings.builder()
                .kafkaTopic(kafka == null ? null : scalarOf(kafka.get(Keyword.TOPIC)))
                .amqpIs(amqp == null ? null : scalarOf(amqp.get(Keyword.IS)))
                //an AMQP name may legitimately be the empty string: that is the default exchange
                .amqpQueue(amqp == null ? null : textOf(childOf(amqp.get(Keyword.QUEUE), "name")))
                .amqpExchange(amqp == null ? null : textOf(childOf(amqp.get(Keyword.EXCHANGE), "name")))
                .wsMethod(ws == null ? null : scalarOf(ws.get(Keyword.METHOD)))
                .raw(raw)
                .build();
    }

    // ------------------------------------------------------------------ operations

    private static AsyncApiOperation parseOperation(
            String name,
            JsonNode rawNode,
            JsonNode root,
            Map<String, AsyncApiChannel> channels,
            Map<String, AsyncApiMessage> messages,
            Map<String, AsyncApiSecurityScheme> securitySchemes,
            List<String> warnings) {

        JsonNode declared = dereference(rawNode, root, warnings);

        if (declared == null) {
            return null;
        }

        JsonNode node = applyTraits(declared, root, Keyword.OPERATION_TRAITS, warnings);

        AsyncApiOperation.Action action = actionOf(node.get(Keyword.ACTION));

        if (action == null) {
            warnings.add(
                    "Operation '" + name + "' declares no valid 'action' (must be 'send' or 'receive'),"
                            + " and is ignored");
            return null;
        }

        String channelRef = AsyncApiRefResolver.refOf(node.get(Keyword.CHANNEL));
        String channelName = channelRef == null
                ? null
                : AsyncApiRefResolver.refKey(channelRef, CHANNEL_REF_PREFIX);

        if (channelName == null || !channels.containsKey(channelName)) {
            warnings.add(
                    "Operation '" + name + "' does not refer to a declared channel"
                            + (channelRef == null ? "" : " (reference was '" + channelRef + "')")
                            + ", and is ignored");
            return null;
        }

        AsyncApiChannel channel = channels.get(channelName);
        List<String> messageIds =
                selectMessages(node.get(Keyword.MESSAGES), channel, channels, messages, name, warnings);

        if (messageIds.isEmpty()) {
            warnings.add(
                    "Operation '" + name + "' has no usable message on channel '" + channelName + "',"
                            + " so nothing can be built for it");
        }

        return AsyncApiOperation.builder(name, action, channelName)
                .messageIds(messageIds)
                .reply(parseReply(node.get(Keyword.REPLY), root, channels, messages, name, warnings))
                .security(parseSecurity(
                        node.get(Keyword.SECURITY), "operation '" + name + "'", root, securitySchemes, warnings))
                .bindings(dereferencedFields(node.get(Keyword.BINDINGS), root, warnings))
                .title(scalarOf(node.get(Keyword.TITLE)))
                .summary(scalarOf(node.get(Keyword.SUMMARY)))
                .description(scalarOf(node.get(Keyword.DESCRIPTION)))
                .build();
    }

    private static AsyncApiOperation.Action actionOf(JsonNode node) {

        String action = scalarOf(node);

        if (action == null) {
            return null;
        }

        switch (action.toLowerCase(Locale.ENGLISH)) {
            case Keyword.SEND:
                return AsyncApiOperation.Action.SEND;
            case Keyword.RECEIVE:
                return AsyncApiOperation.Action.RECEIVE;
            default:
                return null;
        }
    }

    private static AsyncApiReply parseReply(
            JsonNode rawNode,
            JsonNode root,
            Map<String, AsyncApiChannel> channels,
            Map<String, AsyncApiMessage> messages,
            String operationName,
            List<String> warnings) {

        if (rawNode == null || rawNode.isNull()) {
            return null;
        }

        JsonNode node = dereference(rawNode, root, warnings);

        if (node == null) {
            return null;
        }

        String channelRef = AsyncApiRefResolver.refOf(node.get(Keyword.CHANNEL));
        String channelName = channelRef == null
                ? null
                : AsyncApiRefResolver.refKey(channelRef, CHANNEL_REF_PREFIX);

        AsyncApiChannel replyChannel = channelName == null ? null : channels.get(channelName);

        if (channelName != null && replyChannel == null) {
            warnings.add(
                    "The reply of operation '" + operationName + "' refers to channel '" + channelName
                            + "', which is not declared");
        }

        List<String> messageIds = replyChannel == null
                ? new ArrayList<String>()
                : selectMessages(node.get(Keyword.MESSAGES), replyChannel, channels, messages, operationName, warnings);

        //the address may be declared here or shared through components.replyAddresses
        JsonNode rawAddress = node.get(Keyword.ADDRESS);
        JsonNode address = rawAddress == null ? null : dereference(rawAddress, root, warnings);

        return new AsyncApiReply(
                replyChannel == null ? null : replyChannel.getName(),
                messageIds,
                address == null ? null : scalarOf(address.get(Keyword.LOCATION)));
    }

    /**
     * The messages an operation, or a reply, actually carries.
     *
     * A {@code messages} array picks out a subset of what the channel offers; without one,
     * every message of the channel is in play. Entries normally address the channel
     * ({@code #/channels/<c>/messages/<k>}) but a direct reference to a component message is
     * accepted too, as documents in the wild write both.
     */
    private static List<String> selectMessages(
            JsonNode node,
            AsyncApiChannel channel,
            Map<String, AsyncApiChannel> channels,
            Map<String, AsyncApiMessage> messages,
            String owner,
            List<String> warnings) {

        if (node == null || !node.isArray() || node.size() == 0) {
            return new ArrayList<>(channel.getMessageIds());
        }

        /*
            Note there is no falling back to the whole channel when nothing here can be
            resolved: the operation narrowed the set deliberately, and quietly widening it again
            would have it drive messages it never claimed to.
         */
        Set<String> selected = new LinkedHashSet<>();

        for (String ref : refsOf(node)) {

            /*
                Both forms have to be checked against what actually survived parsing, not just
                turned into a key: a message may have been dropped for being unreadable, and an
                operation left holding its id would look drivable while having nothing to send.
             */
            String id = AsyncApiRefResolver.refKey(ref, MESSAGE_REF_PREFIX);

            if (id == null) {
                id = channelScopedMessage(ref, channels);
            }

            if (id != null && messages.containsKey(id)) {
                selected.add(id);
                continue;
            }

            if (isChannelScopedMessageRef(ref) || ref.startsWith(MESSAGE_REF_PREFIX)) {
                warnings.add(
                        "Operation '" + owner + "' selects message '" + ref + "', which is not available"
                                + " (it may itself have been skipped)");
            } else {
                warnings.add(
                        "Operation '" + owner + "' selects a message with unsupported reference '"
                                + ref + "'");
            }
        }

        return new ArrayList<>(selected);
    }

    /**
     * Resolve {@code #/channels/<channel>/messages/<localKey>} to a message id.
     */
    private static String channelScopedMessage(String ref, Map<String, AsyncApiChannel> channels) {

        String[] segments = channelScopedMessageSegments(ref);

        if (segments == null) {
            return null;
        }

        AsyncApiChannel channel = channels.get(segments[0]);

        return channel == null ? null : channel.getMessageKeys().get(segments[2]);
    }

    /**
     * Whether a reference addresses a message through its channel, whatever it resolves to.
     */
    private static boolean isChannelScopedMessageRef(String ref) {
        return channelScopedMessageSegments(ref) != null;
    }

    private static String[] channelScopedMessageSegments(String ref) {

        if (!ref.startsWith(CHANNEL_REF_PREFIX)) {
            return null;
        }

        String[] segments = ref.substring(CHANNEL_REF_PREFIX.length()).split("/", -1);

        return segments.length == 3 && "messages".equals(segments[1]) ? segments : null;
    }

    // ------------------------------------------------------------------ shared helpers

    /**
     * Follow a {@code $ref}, if the node is one. Returns null when it cannot be followed, having
     * said so.
     *
     * A reference may point at another reference, so this recurses -- and documents do contain
     * reference cycles, by mistake or through a generator, so the set of references already
     * seen stops it going round for ever. Without it a two-entry cycle takes down the whole run
     * with a StackOverflowError.
     */
    private static JsonNode dereference(JsonNode node, JsonNode root, List<String> warnings) {
        return dereference(node, root, warnings, new LinkedHashSet<String>());
    }

    private static JsonNode dereference(
            JsonNode node,
            JsonNode root,
            List<String> warnings,
            Set<String> seen) {

        String ref = AsyncApiRefResolver.refOf(node);

        if (ref == null) {
            return node;
        }

        if (!seen.add(ref)) {
            warnings.add("Reference '" + ref + "' is part of a cycle of references, and cannot be followed");
            return null;
        }

        JsonNode resolved = AsyncApiRefResolver.resolveLocal(root, ref);

        if (resolved == null) {
            warnings.add("Could not resolve reference '" + ref + "'");
            return null;
        }

        if (AsyncApiRefResolver.refOf(resolved) != null) {
            return dereference(resolved, root, warnings, seen);
        }

        return resolved;
    }

    /**
     * Every field of a node, with any {@code $ref} followed -- both one on the node itself and
     * one on each of its values. Bindings are routinely shared through {@code components}, and
     * reading them without following the reference is worse than not reading them at all.
     *
     * Key is the field name exactly as the document declares it, so what it means depends on
     * what is being read: a protocol name such as "kafka" for a {@code bindings} node, and a
     * parameter name for a {@code parameters} one. Value is that field's node, dereferenced.
     */
    private static Map<String, JsonNode> dereferencedFields(
            JsonNode node,
            JsonNode root,
            List<String> warnings) {

        if (node == null) {
            return new LinkedHashMap<>();
        }

        JsonNode resolved = dereference(node, root, warnings);

        if (resolved == null) {
            return new LinkedHashMap<>();
        }

        Map<String, JsonNode> fields = new LinkedHashMap<>();

        for (Map.Entry<String, JsonNode> entry : objectFieldsOf(resolved).entrySet()) {
            JsonNode value = dereference(entry.getValue(), root, warnings);
            fields.put(entry.getKey(), value == null ? entry.getValue() : value);
        }

        return fields;
    }

    /**
     * Fold a {@code traits} array into the object that declares it.
     *
     * Traits are a way of writing shared boilerplate once. They are merged shallowly and in
     * declaration order, and whatever the object states itself always wins, which is what the
     * specification prescribes.
     */
    private static JsonNode applyTraits(
            JsonNode node,
            JsonNode root,
            String componentKind,
            List<String> warnings) {

        JsonNode traits = node.get(Keyword.TRAITS);

        if (traits == null || !traits.isArray() || traits.size() == 0) {
            return node;
        }

        List<JsonNode> sources = new ArrayList<>();

        for (JsonNode trait : traits) {

            JsonNode target = dereference(trait, root, warnings);

            if (target == null) {
                /*
                    Nothing to add: dereference has already reported which reference failed and
                    why, and naming the same failure a second time in vaguer terms would only
                    make the list of warnings harder to read.
                 */
                continue;
            }

            if (!target.isObject()) {
                warnings.add("A trait declared in " + componentKind + " is not an object, and is ignored");
            } else {
                sources.add(target);
            }
        }

        //the object's own fields go last, so that what it states itself always wins
        sources.add(node);

        return shallowMerge(sources.toArray(new JsonNode[0]));
    }

    /**
     * The given objects merged one field deep, later ones winning, {@code $ref} dropped
     * throughout.
     *
     * The reference has already been followed by the time anything is merged, so carrying it
     * into the result would only make a later reader follow it again and discard everything
     * that was merged on top.
     */
    private static JsonNode shallowMerge(JsonNode... sources) {

        ObjectNode merged = JsonNodeFactory.instance.objectNode();

        for (JsonNode source : sources) {
            Iterator<Map.Entry<String, JsonNode>> fields = source.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!Keyword.REF.equals(field.getKey())) {
                    merged.set(field.getKey(), field.getValue());
                }
            }
        }

        return merged;
    }

    /**
     * How a node that was expected to be a schema should be named in a warning, so that the
     * user can recognise it in their own document.
     */
    private static String describe(JsonNode node) {

        if (node.isNull()) {
            return "null";
        }

        if (node.isArray()) {
            return "an array";
        }

        if (node.isBoolean()) {
            return "the boolean " + node.asText();
        }

        return "the value '" + node.asText() + "'";
    }

    /**
     * The value of a scalar field as text, or null when it is absent, explicitly null, blank,
     * or not a scalar. Numbers and booleans are rendered as written, which matters for fields
     * that are free text but often look numeric.
     */
    private static String scalarOf(JsonNode node) {

        String text = textOf(node);

        return text == null || text.trim().isEmpty() ? null : text;
    }

    /**
     * As {@link #scalarOf}, with a fallback for when there is nothing to read.
     */
    private static String scalarOr(JsonNode node, String fallback) {

        String value = scalarOf(node);

        return value == null ? fallback : value;
    }

    /**
     * As {@link #scalarOf}, but keeping a value that is there and empty. Only a few fields can
     * mean something by an empty string -- AMQP's default exchange is named "" -- so this is the
     * exception rather than the rule.
     */
    private static String textOf(JsonNode node) {

        if (node == null || node.isNull() || node.isContainerNode()) {
            return null;
        }

        return node.asText();
    }

    private static JsonNode childOf(JsonNode node, String field) {
        return node == null ? null : node.get(field);
    }

    private static List<String> scalarsOf(JsonNode node) {

        List<String> values = new ArrayList<>();

        if (node == null || !node.isArray()) {
            return values;
        }

        for (JsonNode entry : node) {
            String value = scalarOf(entry);
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    /**
     * The {@code $ref} of every entry of an array.
     */
    private static List<String> refsOf(JsonNode node) {

        List<String> refs = new ArrayList<>();

        if (node == null || !node.isArray()) {
            return refs;
        }

        for (JsonNode entry : node) {
            String ref = AsyncApiRefResolver.refOf(entry);
            if (ref != null) {
                refs.add(ref);
            }
        }

        return refs;
    }

    /**
     * The object entries of an array, which is the only shape {@code examples} is read in.
     */
    private static List<JsonNode> objectsOf(JsonNode node) {

        List<JsonNode> objects = new ArrayList<>();

        if (node == null || !node.isArray()) {
            return objects;
        }

        for (JsonNode entry : node) {
            if (entry.isObject()) {
                objects.add(entry);
            }
        }

        return objects;
    }

    /**
     * The fields of an object node, in declaration order. Empty when it is absent or is not an
     * object, so that callers never have to check first.
     */
    private static Map<String, JsonNode> objectFieldsOf(JsonNode node) {

        Map<String, JsonNode> fields = new LinkedHashMap<>();

        if (node == null || !node.isObject()) {
            return fields;
        }

        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> field = iterator.next();
            fields.put(field.getKey(), field.getValue());
        }

        return fields;
    }

    /**
     * The entries of one {@code components} block.
     */
    private static Map<String, JsonNode> componentsOf(JsonNode root, String kind) {

        JsonNode components = root.get(Keyword.COMPONENTS);

        return objectFieldsOf(components == null ? null : components.get(kind));
    }
}
