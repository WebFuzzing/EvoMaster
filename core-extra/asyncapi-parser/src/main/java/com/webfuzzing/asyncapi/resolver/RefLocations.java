package com.webfuzzing.asyncapi.resolver;

/**
 * Working out what a {@code $ref} points at.
 *
 * A reference is a location followed by a {@code #} and a JSON Pointer. An empty location means
 * the document making the reference, which is the only kind that can be followed without
 * retrieving anything.
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
}
