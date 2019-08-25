package org.evomaster.client.java.instrumentation.shared;

import java.util.Objects;

public class TaintInputName {

    private static final String PREFIX = "evomaster_";

    private static final String POSTFIX = "_input";

    /**
     * Check if a given string value is a tainted value
     */
    public static boolean isTaintInput(String value){
        if(value == null){
            return false;
        }
        return value.toLowerCase().startsWith(PREFIX) &&
                value.toLowerCase().endsWith(POSTFIX);
    }

    /**
     * Create a tainted value, with the input id being part of it
     */
    public static String getTaintName(String id){
        Objects.requireNonNull(id);
        /*
            Note: this is quite simple, we simply add a unique prefix
            and postfix, in lowercase.
            But we would not be able to check if the part of the id was
            modified.
         */
        return (PREFIX + id + POSTFIX).toLowerCase();
    }
}
