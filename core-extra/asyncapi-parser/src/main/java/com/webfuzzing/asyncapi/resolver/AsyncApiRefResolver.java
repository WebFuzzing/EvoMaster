package com.webfuzzing.asyncapi.resolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.models.DocumentLocationType;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * {@code $ref} handling for AsyncAPI documents.
 *
 * There are two quite different jobs here, and they are treated differently on purpose.
 *
 * References <i>between AsyncAPI constructs</i> (a message pointing at a trait, a message
 * pointing at a correlation id) are resolved by {@link #resolveLocal} while parsing: the model
 * that comes out holds plain keys, never pointers.
 *
 * References <i>inside a JSON Schema</i> are not resolved at all, because whatever consumes
 * them later would rather resolve them itself. What is done instead is to make sure it can:
 * every schema a payload can reach has to sit in one flat map under
 * {@code #/components/schemas/}. That is why a document referenced from another is
 * <i>inlined</i> by {@link #inlineExternalDocuments} rather than merely linked -- its schemas
 * are copied in under a namespaced key, and the pointers that referred to it are rewritten to
 * match.
 */
public class AsyncApiRefResolver {

    private static final String REF = "$ref";

    private static final String COMPONENTS = "components";

    private static final String SCHEMAS = "schemas";

    private static final String MESSAGES = "messages";

    public static final String SCHEMA_PREFIX = RefLocations.FRAGMENT_SEPARATOR + RefLocations.PATH_SEPARATOR
            + COMPONENTS + RefLocations.PATH_SEPARATOR + SCHEMAS + RefLocations.PATH_SEPARATOR;

    /**
     * The two component kinds that are worth pulling out of an external document. Schemas are
     * the point of the exercise; messages come along because a document that splits its schemas
     * out often splits its messages out too.
     */
    private static final List<String> INLINABLE = Arrays.asList(SCHEMAS, MESSAGES);

    /**
     * A ceiling on how many other documents one document may drag in. Documents reference each
     * other by path, and a server that answers every path -- or a symlink loop -- would
     * otherwise be followed forever.
     */
    private static final int MAX_IMPORTED_DOCUMENTS = 100;

    /**
     * JSON Pointer escaping: a "/" inside a key is written "~1", and a "~" is written "~0".
     */
    private static final String ESCAPED_SLASH = "~1";

    private static final String TILDE = "~";

    private static final String ESCAPED_TILDE = TILDE + "0";

    /**
     * How the text of a retrieved document is turned into a tree. Supplied by the caller so
     * that this class does not have to know which reader is in use.
     */
    @FunctionalInterface
    public interface DocumentReader {
        JsonNode readTree(String text) throws IOException;
    }

    private AsyncApiRefResolver() {
    }

    public static boolean isLocal(String ref) {
        return RefLocations.isLocalRef(ref);
    }

    /**
     * Follow a local {@code $ref} such as {@code #/components/messages/foo} inside
     * {@code root}. Returns null when the pointer does not lead anywhere.
     */
    public static JsonNode resolveLocal(JsonNode root, String ref) {

        if (!isLocal(ref)) {
            return null;
        }

        JsonNode current = root;

        for (String segment : ref.substring(RefLocations.FRAGMENT_SEPARATOR.length())
                .split(RefLocations.PATH_SEPARATOR)) {

            if (segment.isEmpty()) {
                continue;
            }

            current = current.get(decodePointerSegment(segment));

            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * The last segment of a {@code $ref}, i.e. the key of what it points at, but only if the
     * pointer has the shape we expect.
     * {@code refKey("#/components/messages/foo", "#/components/messages/")} gives "foo", while
     * a pointer somewhere else gives null.
     */
    public static String refKey(String ref, String expectedPrefix) {

        if (!ref.startsWith(expectedPrefix)) {
            return null;
        }

        String key = ref.substring(expectedPrefix.length());

        //must be a single segment: a deeper pointer is something else than what was asked for
        if (key.trim().isEmpty() || key.contains(RefLocations.PATH_SEPARATOR)) {
            return null;
        }

        return decodePointerSegment(key);
    }

    /**
     * The value of a node's {@code $ref}, or null when it has none.
     */
    public static String refOf(JsonNode node) {

        if (node == null) {
            return null;
        }

        JsonNode ref = node.get(REF);

        return ref != null && ref.isTextual() ? ref.asText() : null;
    }

    /**
     * The component schema a reference reaches into, or null if it addresses something else.
     *
     * Unlike {@link #refKey} this accepts a pointer that goes deeper than the schema itself,
     * such as {@code #/components/schemas/Order/properties/item}: the schema named by the first
     * segment is still what has to be present for the pointer to lead anywhere.
     */
    public static String schemaKeyOf(String ref) {

        if (!ref.startsWith(SCHEMA_PREFIX)) {
            return null;
        }

        String rest = ref.substring(SCHEMA_PREFIX.length());
        int slash = rest.indexOf('/');
        String key = slash < 0 ? rest : rest.substring(0, slash);

        return key.trim().isEmpty() ? null : decodePointerSegment(key);
    }

    /**
     * Copy into {@code root} every schema (and message) that its {@code $ref} reach in other
     * documents, so that afterwards the document is self-contained and every reference in it is
     * local.
     *
     * Imported components are keyed {@code _ext_<hash>_<originalKey>}, the hash being derived
     * from the source location. That keeps them from colliding with the primary document's own
     * components, and keeps the key stable across runs so generated output stays diffable.
     *
     * Documents referenced from documents that were themselves imported are followed too.
     * Anything that cannot be retrieved, or that is referenced in a way not supported here, is
     * reported in {@code warnings} and left alone: the affected message becomes unusable, but
     * the rest of the document is still perfectly usable.
     */
    public static void inlineExternalDocuments(
            ObjectNode root,
            DocumentLocation primaryLocation,
            List<String> warnings,
            AsyncApiDocumentFetcher fetch,
            DocumentReader reader) {

        if (primaryLocation.getType() == DocumentLocationType.MEMORY) {
            //nothing to resolve against: a document given as text has no neighbours
            List<String> external = externalRefsOf(root);
            if (!external.isEmpty()) {
                warnings.add(
                        "The document was supplied as text, so its " + external.size() + " reference(s)"
                                + " to other documents cannot be resolved, e.g. '" + external.get(0) + "'");
            }
            return;
        }

        //key is the absolute location of an imported document, value is that document
        Map<String, LoadedDocument> loaded = new LinkedHashMap<>();

        //locations already dealt with, whether imported or failed
        Set<String> settled = new LinkedHashSet<>();

        //a document naming itself still needs its references turned back into local ones
        boolean namesItself = false;

        //breadth-first, since an imported document may import further documents itself
        Deque<PendingDocument> pending = new ArrayDeque<>();
        pending.add(new PendingDocument(root, primaryLocation));

        while (!pending.isEmpty()) {

            PendingDocument current = pending.removeFirst();

            for (String ref : externalRefsOf(current.root)) {

                String absolute = locationOf(ref, current.location, warnings);

                if (absolute == null) {
                    continue;
                }

                if (absolute.equals(primaryLocation.getLocation())) {
                    //not another document at all: this one, named the long way
                    namesItself = true;
                    continue;
                }

                if (!settled.add(absolute)) {
                    continue;
                }

                if (loaded.size() >= MAX_IMPORTED_DOCUMENTS) {
                    warnings.add(
                            "More than " + MAX_IMPORTED_DOCUMENTS + " documents are referenced from this"
                                    + " one. '" + ref + "' and any further reference are ignored.");
                    continue;
                }

                DocumentLocationType type = locationTypeOf(absolute, current.location);

                JsonNode other;
                try {
                    other = reader.readTree(fetch.fetch(absolute, type));
                } catch (Exception e) {
                    warnings.add(
                            "Failed to retrieve the document referenced as '" + ref + "': " + e.getMessage());
                    continue;
                }

                DocumentLocation otherLocation = new DocumentLocation(absolute, type);
                loaded.put(absolute, new LoadedDocument(other, prefixFor(absolute), otherLocation));
                pending.add(new PendingDocument(other, otherLocation));
            }
        }

        if (loaded.isEmpty() && !namesItself) {
            return;
        }

        /*
            Order matters here. The primary document is rewritten first, while its tree still
            holds only its own nodes: once the imported ones have been copied in, a walk of the
            primary tree would reach them too and would resolve their relative references
            against the wrong document -- quietly binding them to whatever happened to be there.
         */
        rewriteRefs(root, primaryLocation, primaryLocation.getLocation(), null, loaded, warnings);

        for (LoadedDocument doc : loaded.values()) {
            for (String kind : INLINABLE) {
                JsonNode components = componentsOf(doc.root, kind);
                if (components == null) {
                    continue;
                }
                for (JsonNode value : components) {
                    rewriteRefs(value, doc.location, primaryLocation.getLocation(), doc.prefix, loaded, warnings);
                }
            }
        }

        for (LoadedDocument doc : loaded.values()) {
            for (String kind : INLINABLE) {
                JsonNode imported = componentsOf(doc.root, kind);
                if (imported == null) {
                    continue;
                }
                ObjectNode target = ensureObject(ensureObject(root, COMPONENTS), kind);
                Iterator<Map.Entry<String, JsonNode>> fields = imported.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    target.set(doc.prefix + field.getKey(), field.getValue());
                }
            }
        }
    }

    /**
     * Every {@code $ref} value under {@code node}, at any depth.
     */
    public static List<String> collectRefs(JsonNode node) {
        List<String> refs = new ArrayList<>();
        collectRefsInto(node, refs);
        return refs;
    }

    /**
     * The distinct references under {@code node} that lead outside the document holding it.
     */
    private static List<String> externalRefsOf(JsonNode node) {

        Set<String> external = new LinkedHashSet<>();

        for (String ref : collectRefs(node)) {
            if (!isLocal(ref)) {
                external.add(ref);
            }
        }

        return new ArrayList<>(external);
    }

    /**
     * Where a reference points, as an absolute location.
     *
     * The resolution can throw on input it was not written for -- a protocol-relative
     * {@code //host/path} reference read from a plain file path, for one. Nothing here is worth
     * failing a whole document over.
     */
    private static String locationOf(String ref, DocumentLocation from, List<String> warnings) {
        try {
            return RefLocations.computeLocation(ref, from, warnings);
        } catch (Exception e) {
            warnings.add("Cannot work out what document '" + ref + "' refers to: " + e.getMessage());
            return null;
        }
    }

    /**
     * One document pulled in from elsewhere.
     */
    private static class LoadedDocument {

        private final JsonNode root;
        private final String prefix;
        private final DocumentLocation location;

        private LoadedDocument(JsonNode root, String prefix, DocumentLocation location) {
            this.root = root;
            this.prefix = prefix;
            this.location = location;
        }
    }

    /**
     * A document whose references have not been walked yet.
     */
    private static class PendingDocument {

        private final JsonNode root;
        private final DocumentLocation location;

        private PendingDocument(JsonNode root, DocumentLocation location) {
            this.root = root;
            this.location = location;
        }
    }

    /**
     * Point every {@code $ref} under {@code node} at the inlined copy of what it referred to.
     *
     * {@code base} is the document the node belongs to, which is what a relative reference is
     * relative to. {@code ownerPrefix} is set only when the node came from an imported document:
     * a plain {@code #/components/schemas/X} written in there means <i>that</i> document's X,
     * which is now stored under the document's own prefix. The primary document's own local
     * references are already correct and are left alone.
     *
     * {@code loaded} is keyed by the absolute location of each imported document, its value
     * being that document and the prefix its components were copied in under.
     */
    private static void rewriteRefs(
            JsonNode node,
            DocumentLocation base,
            String primary,
            String ownerPrefix,
            Map<String, LoadedDocument> loaded,
            List<String> warnings) {

        if (node.isObject()) {

            ObjectNode obj = (ObjectNode) node;
            String ref = refOf(obj);

            if (ref != null) {
                String rewritten = rewrite(ref, base, primary, ownerPrefix, loaded, warnings);
                if (rewritten != null) {
                    obj.put(REF, rewritten);
                }
            }

            for (JsonNode value : obj) {
                rewriteRefs(value, base, primary, ownerPrefix, loaded, warnings);
            }

        } else if (node.isArray()) {
            for (JsonNode value : node) {
                rewriteRefs(value, base, primary, ownerPrefix, loaded, warnings);
            }
        }
    }

    /**
     * The new value for a single {@code $ref}, or null when it needs no change.
     */
    private static String rewrite(
            String ref,
            DocumentLocation base,
            String primary,
            String ownerPrefix,
            Map<String, LoadedDocument> loaded,
            List<String> warnings) {

        if (isLocal(ref)) {
            //a local reference inside an imported document now has to name the imported copy
            return ownerPrefix == null ? null : renameComponent(fragmentOf(ref), ownerPrefix, ref, warnings);
        }

        String absolute = locationOf(ref, base, warnings);

        if (absolute == null) {
            return null;
        }

        /*
            Some generators write even a reference that stays inside the document as an absolute
            one. It names this very file, so it is just a local reference written the long way,
            and turning it back into one is all that is needed.
         */
        if (absolute.equals(primary)) {
            String fragment = fragmentOf(ref);
            return fragment.trim().isEmpty() ? null : RefLocations.FRAGMENT_SEPARATOR + fragment;
        }

        LoadedDocument target = loaded.get(absolute);

        if (target == null) {
            return null;
        }

        String fragment = fragmentOf(ref);

        if (fragment.trim().isEmpty()) {
            warnings.add(
                    "Reference '" + ref + "' points at a whole document rather than at a component of"
                            + " it, which is not supported");
            return null;
        }

        return renameComponent(fragment, target.prefix, ref, warnings);
    }

    /**
     * Whatever follows the first {@code #} of a reference, empty when there is none.
     */
    private static String fragmentOf(String ref) {
        int hash = ref.indexOf(RefLocations.FRAGMENT_SEPARATOR);
        return hash < 0 ? "" : ref.substring(hash + 1);
    }

    /**
     * Turn the fragment of a reference into a local pointer at the inlined copy.
     *
     * Only the component's own key is renamed. A pointer may go deeper than the component --
     * {@code #/components/schemas/Order/properties/item} addresses one property of a schema --
     * and everything past the key describes a way *into* the imported node, which the rename
     * must carry through untouched. Dropping the tail instead would leave the reference naming
     * the primary document's own component of that name, silently binding a payload to an
     * unrelated schema.
     */
    private static String renameComponent(
            String fragment,
            String prefix,
            String original,
            List<String> warnings) {

        String path = fragment.startsWith(RefLocations.PATH_SEPARATOR)
                ? fragment.substring(RefLocations.PATH_SEPARATOR.length())
                : fragment;
        String[] segments = path.split(RefLocations.PATH_SEPARATOR, -1);

        if (segments.length < 3 || !COMPONENTS.equals(segments[0]) || !INLINABLE.contains(segments[1])) {
            warnings.add(
                    "Reference '" + original + "' points at '" + path + "' of another document."
                            + " Only components/" + SCHEMAS + " and components/" + MESSAGES
                            + " can be imported.");
            return null;
        }

        StringBuilder renamed = new StringBuilder("#/")
                .append(COMPONENTS).append('/')
                .append(segments[1]).append('/')
                .append(prefix).append(segments[2]);

        for (int i = 3; i < segments.length; i++) {
            renamed.append('/').append(segments[i]);
        }

        return renamed.toString();
    }

    /**
     * A short, deterministic namespace for the components of one external document, so that
     * they cannot collide with the primary document's own.
     */
    private static String prefixFor(String absoluteLocation) {

        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-1")
                    .digest(absoluteLocation.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            //SHA-1 is required of every JVM, so this cannot happen
            throw new IllegalStateException(e);
        }

        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            hex.append(String.format("%02x", digest[i]));
        }

        return "_ext_" + hex + "_";
    }

    private static JsonNode componentsOf(JsonNode root, String kind) {

        JsonNode components = root.get(COMPONENTS);

        if (components == null) {
            return null;
        }

        JsonNode of = components.get(kind);

        return of != null && of.isObject() ? of : null;
    }

    private static ObjectNode ensureObject(ObjectNode parent, String field) {

        JsonNode existing = parent.get(field);

        if (existing != null && existing.isObject()) {
            return (ObjectNode) existing;
        }

        return parent.putObject(field);
    }

    private static DocumentLocationType locationTypeOf(String absoluteLocation, DocumentLocation from) {

        String lower = absoluteLocation.toLowerCase(Locale.ENGLISH);

        if (lower.startsWith("http:") || lower.startsWith("https:")) {
            return DocumentLocationType.REMOTE;
        }

        //a document read off the classpath can only reference other classpath documents
        if (from.getType() == DocumentLocationType.RESOURCE) {
            return DocumentLocationType.RESOURCE;
        }

        return DocumentLocationType.LOCAL;
    }

    private static void collectRefsInto(JsonNode node, List<String> out) {

        if (node.isObject()) {

            String ref = refOf(node);

            if (ref != null) {
                out.add(ref);
            }

            for (JsonNode value : node) {
                collectRefsInto(value, out);
            }

        } else if (node.isArray()) {
            for (JsonNode value : node) {
                collectRefsInto(value, out);
            }
        }
    }

    /**
     * JSON Pointer escaping: "~1" is a "/" and "~0" is a "~". Percent-encoding is undone too,
     * as references are URIs.
     */
    private static String decodePointerSegment(String segment) {

        String decoded = segment;

        //only when there is something to decode: URLDecoder would otherwise eat a literal '+'
        if (segment.indexOf('%') >= 0) {
            try {
                decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                decoded = segment;
            }
        }

        return decoded.replace(ESCAPED_SLASH, RefLocations.PATH_SEPARATOR).replace(ESCAPED_TILDE, TILDE);
    }
}
