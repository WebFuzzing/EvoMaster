package com.webfuzzing.asyncapi.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.webfuzzing.asyncapi.mapper.AsyncApiMapper;
import com.webfuzzing.asyncapi.models.AsyncApiCorrelationId;
import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.models.AsyncApiMessage;
import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.resolver.AsyncApiRefResolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

    private static final String KAFKA = "kafka";

    private static final String REF = "$ref";

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
     * Parse {@code schemaText}, which was retrieved from {@code location}.
     */
    public static AsyncApiDocument parse(String schemaText, DocumentLocation location) {

        JsonNode root;
        try {
            root = AsyncApiMapper.readTree(schemaText);
        } catch (Exception e) {
            throw new AsyncApiParsingException("Failed to parse the AsyncAPI document: " + e.getMessage(), e);
        }

        if (root == null || !root.isObject()) {
            throw new AsyncApiParsingException("The AsyncAPI document is not a JSON/YAML object");
        }

        String version = scalarOf(root.get("asyncapi"));

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

        String defaultContentType = scalarOf(root.get("defaultContentType"));
        if (defaultContentType == null) {
            defaultContentType = AsyncApiDocument.DEFAULT_CONTENT_TYPE;
        }

        Map<String, JsonNode> componentSchemas = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : componentsOf(root, "schemas").entrySet()) {
            JsonNode schema = schemaOf(entry.getValue(), "the component schema '" + entry.getKey() + "'", warnings);
            if (schema != null) {
                componentSchemas.put(entry.getKey(), schema);
            }
        }

        Map<String, AsyncApiMessage> messages = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : componentsOf(root, "messages").entrySet()) {
            AsyncApiMessage message = parseMessage(
                    entry.getKey(), entry.getValue(), root, defaultContentType, componentSchemas, warnings);
            if (message != null) {
                messages.put(entry.getKey(), message);
            }
        }

        return AsyncApiDocument.builder(schemaText, location, version)
                .defaultContentType(defaultContentType)
                .messages(messages)
                .componentSchemas(componentSchemas)
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

        JsonNode node = applyTraits(declared, root, "messageTraits", warnings);

        JsonNode payload = schemaOf(node.get("payload"), "message '" + id + "'", warnings);

        if (payload != null && reportUnfollowable(
                payload, componentSchemas, "message '" + id + "'", "The message is ignored.", warnings)) {
            payload = null;
        }

        if (payload == null && node.has("payload")) {
            //nothing can be built from a payload that cannot be read, so the message goes too
            return null;
        }

        //broken headers cost only the headers: the message itself is still usable
        JsonNode headers = schemaOf(node.get("headers"), "the headers of message '" + id + "'", warnings);

        if (headers != null && reportUnfollowable(
                headers, componentSchemas, "the headers of message '" + id + "'",
                "The headers are ignored.", warnings)) {
            headers = null;
        }

        Map<String, JsonNode> bindings = dereferencedFields(node.get("bindings"), root, warnings);
        JsonNode kafka = bindings.get(KAFKA);

        return AsyncApiMessage.builder(id)
                .name(scalarOr(node.get("name"), id))
                .contentType(scalarOr(node.get("contentType"), defaultContentType))
                .payload(payload)
                .headers(headers)
                .correlationId(parseCorrelationId(node.get("correlationId"), root, id, warnings))
                .kafkaKey(kafka == null ? null : kafka.get("key"))
                .bindings(bindings)
                .examples(objectsOf(node.get("examples")))
                .title(scalarOf(node.get("title")))
                .summary(scalarOf(node.get("summary")))
                .description(scalarOf(node.get("description")))
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

        String format = scalarOf(node.get("schemaFormat"));

        if (format == null) {
            return node;
        }

        for (String known : JSON_SCHEMA_FORMATS) {
            if (format.startsWith(known)) {
                //a multi-format declaration keeps the schema itself one level down
                JsonNode schema = node.get("schema");
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

        String location = scalarOf(node.get("location"));

        if (location == null) {
            warnings.add(
                    "Message '" + messageId + "' declares a correlationId with no 'location', so there"
                            + " is no way to know where the id travels");
            return null;
        }

        AsyncApiCorrelationId parsed =
                AsyncApiCorrelationId.parse(location, scalarOf(node.get("description")));

        if (parsed == null) {
            warnings.add(
                    "Message '" + messageId + "' declares the correlation id at '" + location + "',"
                            + " which is not a supported runtime expression. It must point inside the"
                            + " message header or payload.");
        }

        return parsed;
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

        JsonNode traits = node.get("traits");

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
                if (!REF.equals(field.getKey())) {
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

        if (node == null || node.isNull() || node.isContainerNode()) {
            return null;
        }

        String text = node.asText();

        return text.trim().isEmpty() ? null : text;
    }

    /**
     * As {@link #scalarOf}, with a fallback for when there is nothing to read.
     */
    private static String scalarOr(JsonNode node, String fallback) {

        String value = scalarOf(node);

        return value == null ? fallback : value;
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

        JsonNode components = root.get("components");

        return objectFieldsOf(components == null ? null : components.get(kind));
    }
}
