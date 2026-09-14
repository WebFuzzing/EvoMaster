package com.webfuzzing.asyncapi.resolver;

import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.models.DocumentLocationType;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Working out where the document a {@code $ref} points at actually lives.
 *
 * A reference is a location followed by a {@code #} and a JSON Pointer, and the location part
 * is written relative to the document making the reference. Turning that into something that
 * can be retrieved is all this does.
 *
 * @see <a href="https://www.asyncapi.com/docs/reference/specification/v3.0.0#referenceObject">
 *     Reference Object</a>
 */
public class RefLocations {

    /**
     * Separates the document location from the JSON Pointer inside a {@code $ref}.
     */
    public static final String FRAGMENT_SEPARATOR = "#";

    /**
     * Separates the segments of a JSON Pointer, and of a path.
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * Separates the protocol from the rest of a location, as in "https:".
     */
    private static final String PROTOCOL_SEPARATOR = ":";

    /**
     * A location naming a host but no protocol, which borrows the protocol of the document
     * referring to it.
     */
    private static final String PROTOCOL_RELATIVE_PREFIX = "//";

    /**
     * The schemes whose locations are already absolute, so need no resolving.
     */
    private static final String HTTP_SCHEME = "http";

    private static final String HTTPS_SCHEME = "https";

    private static final String HTTP_PREFIX = HTTP_SCHEME + PROTOCOL_SEPARATOR;

    private static final String HTTPS_PREFIX = HTTPS_SCHEME + PROTOCOL_SEPARATOR;

    private RefLocations() {
    }

    /**
     * Whether the location is an absolute http(s) URL, and so is already where the document
     * lives rather than something to resolve against the document referring to it.
     */
    public static boolean isHttpLocation(String location) {

        String lower = location.toLowerCase(Locale.ENGLISH);

        return lower.startsWith(HTTP_PREFIX) || lower.startsWith(HTTPS_PREFIX);
    }

    /**
     * Whether the reference stays inside the document making it.
     */
    public static boolean isLocalRef(String ref) {
        return ref.startsWith(FRAGMENT_SEPARATOR);
    }

    /**
     * The absolute location of the document a reference points at, resolved against the
     * document making the reference. Null when the reference is not one this can make sense of,
     * in which case {@code messages} says why.
     *
     * @throws IllegalArgumentException if the referring document was supplied as text, as there
     *                                  is then nothing for a relative location to be relative to
     */
    public static String resolveDocumentLocation(String ref, DocumentLocation currentSource, List<String> messages) {

        String rawLocation = extractLocationPart(ref, messages);

        if (rawLocation == null) {
            return null;
        }

        if (isHttpLocation(rawLocation)) {
            //location is absolute, so no need to do anything
            return rawLocation;
        }

        if (currentSource.getType() == DocumentLocationType.MEMORY) {
            throw new IllegalArgumentException(
                    "Can't handle relative location for memory files: " + rawLocation);
        }

        String csl = currentSource.getLocation();

        if (rawLocation.startsWith(PROTOCOL_RELATIVE_PREFIX)) {
            //as per specs, use same protocol as source
            int separator = csl.indexOf(PROTOCOL_SEPARATOR);
            if (separator < 0) {
                /*
                    A protocol-relative reference read from something that has no protocol, such
                    as a plain file path. There is nothing to borrow, so the reference cannot be
                    resolved.
                 */
                messages.add("No protocol can be inferred for " + rawLocation + " from " + csl);
                return null;
            }
            return csl.substring(0, separator) + PROTOCOL_SEPARATOR + rawLocation;
        }

        //if we arrive here, it is a relative location
        return resolveRelative(rawLocation, currentSource, messages);
    }

    /**
     * Resolve a relative location against the folder holding the document that refers to it.
     *
     * A URL, a {@code file:} URL and a classpath path are resolved as URIs, per RFC 3986. A
     * plain file path is resolved through the file system instead: it may contain a space, or
     * on Windows backslashes and a drive letter, none of which {@link URI} accepts.
     *
     * Both collapse "." and ".." segments, so two references to the same document produce the
     * same string. That matters because the string is what tells imported documents apart.
     */
    private static String resolveRelative(
            String rawLocation,
            DocumentLocation currentSource,
            List<String> messages) {

        String csl = currentSource.getLocation();

        if (currentSource.isPlainFilePath()) {
            return Paths.get(csl).resolveSibling(rawLocation).normalize().toString();
        }

        try {
            return URI.create(csl).resolve(rawLocation).toString();
        } catch (IllegalArgumentException e) {
            /*
                The referring document's location is a URL or classpath path that is not a legal
                URI -- one written with a space in it, say. There is then no well-defined way to
                resolve a relative reference from it, so this says why rather than guessing.
             */
            messages.add(
                    "Cannot resolve '" + rawLocation + "' against '" + csl + "', which is not a valid"
                            + " URI: " + e.getMessage());
            return null;
        }
    }

    /**
     * The location part of a reference, i.e. everything before the {@code #} that separates it
     * from the JSON Pointer. Empty for a reference that stays inside its own document, and null
     * when there is no separator at all, in which case {@code messages} says so.
     *
     * This only reads the text. Turning what it gives back into somewhere a document can be
     * retrieved from is {@link #resolveDocumentLocation}'s job.
     */
    private static String extractLocationPart(String ref, List<String> messages) {

        if (!ref.contains(FRAGMENT_SEPARATOR)) {
            messages.add("Not a valid $ref, as it contains no " + FRAGMENT_SEPARATOR + ": " + ref);
            return null;
        }

        return ref.substring(0, ref.indexOf(FRAGMENT_SEPARATOR));
    }
}
