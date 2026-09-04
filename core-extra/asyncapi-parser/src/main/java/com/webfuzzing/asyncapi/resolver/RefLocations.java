package com.webfuzzing.asyncapi.resolver;

import com.webfuzzing.asyncapi.models.DocumentLocation;
import com.webfuzzing.asyncapi.models.DocumentLocationType;

import java.net.URI;
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

    private static final String HTTP_PREFIX = "http" + PROTOCOL_SEPARATOR;

    private static final String HTTPS_PREFIX = "https" + PROTOCOL_SEPARATOR;

    /**
     * What a relative location is resolved against, as discussed in the specification: a
     * reference is relative to the folder holding the document that makes it, not to the
     * document itself.
     */
    private static final String PARENT_FOLDER = ".." + PATH_SEPARATOR;

    private RefLocations() {
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
    public static String computeLocation(String ref, DocumentLocation currentSource, List<String> messages) {

        String rawLocation = extractLocation(ref, messages);

        if (rawLocation == null) {
            return null;
        }

        String lower = rawLocation.toLowerCase(Locale.ENGLISH);

        if (lower.startsWith(HTTP_PREFIX) || lower.startsWith(HTTPS_PREFIX)) {
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

        //if arrive here, it is a relative path
        String delimiter = csl.endsWith(PATH_SEPARATOR) ? "" : PATH_SEPARATOR;

        String location = csl + delimiter + PARENT_FOLDER + rawLocation;

        try {
            return new URI(location).normalize().toString();
        } catch (Exception e) {
            return location;
        }
    }

    private static String extractLocation(String ref, List<String> messages) {

        if (!ref.contains(FRAGMENT_SEPARATOR)) {
            messages.add("Not a valid $ref, as it contains no " + FRAGMENT_SEPARATOR + ": " + ref);
            return null;
        }

        return ref.substring(0, ref.indexOf(FRAGMENT_SEPARATOR));
    }
}
