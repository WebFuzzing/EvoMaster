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

    private RefLocations() {
    }

    /**
     * Whether the reference stays inside the document making it.
     */
    public static boolean isLocalRef(String ref) {
        return ref.startsWith("#");
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

        if (lower.startsWith("http:") || lower.startsWith("https:")) {
            //location is absolute, so no need to do anything
            return rawLocation;
        }

        if (currentSource.getType() == DocumentLocationType.MEMORY) {
            throw new IllegalArgumentException(
                    "Can't handle relative location for memory files: " + rawLocation);
        }

        String csl = currentSource.getLocation();

        if (rawLocation.startsWith("//")) {
            //as per specs, use same protocol as source
            int separator = csl.indexOf(':');
            if (separator < 0) {
                /*
                    A protocol-relative reference read from something that has no protocol, such
                    as a plain file path. There is nothing to borrow, so the reference cannot be
                    resolved.
                 */
                messages.add("No protocol can be inferred for " + rawLocation + " from " + csl);
                return null;
            }
            return csl.substring(0, separator) + ":" + rawLocation;
        }

        //if arrive here, it is a relative path
        String delimiter = csl.endsWith("/") ? "" : "/";
        String parentFolder = "../"; // this is based on what is discussed in the specs

        String location = csl + delimiter + parentFolder + rawLocation;

        try {
            return new URI(location).normalize().toString();
        } catch (Exception e) {
            return location;
        }
    }

    private static String extractLocation(String ref, List<String> messages) {

        if (!ref.contains("#")) {
            messages.add("Not a valid $ref, as it contains no #: " + ref);
            return null;
        }

        return ref.substring(0, ref.indexOf('#'));
    }
}
