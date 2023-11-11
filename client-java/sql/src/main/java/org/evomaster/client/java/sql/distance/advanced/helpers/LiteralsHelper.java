package org.evomaster.client.java.sql.distance.advanced.helpers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class used to handle boolean literals due to lack of JSQL parser support.
 */
public class LiteralsHelper {

    public static final Set<String> BOOLEAN_TRUE_LITERALS = new HashSet<>(
        Arrays.asList("t", "true", "yes", "y", "on"));
    public static final Set<String> BOOLEAN_FALSE_LITERALS = new HashSet<>(
        Arrays.asList("f", "false", "no", "n", "off"));
    public static final Set<String> BOOLEAN_LITERALS =
        Stream.concat(BOOLEAN_FALSE_LITERALS.stream(), BOOLEAN_TRUE_LITERALS.stream()).collect(Collectors.toSet());

    public static Boolean isBooleanLiteral(Object object) {
        return BOOLEAN_LITERALS.contains(object.toString().toLowerCase());
    }

    public static Boolean isTrueLiteral(Object object) {
        return BOOLEAN_TRUE_LITERALS.contains(object.toString().toLowerCase());
    }
}