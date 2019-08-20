package org.evomaster.client.java.instrumentation.shared;

import java.util.Objects;

public class TaintInputName {

    private static final String PREFIX = "evomaster_";

    public static boolean isTaintInput(String value){
        if(value == null){
            return false;
        }
        return value.startsWith(PREFIX);
    }

    public static String getTaintName(String postfix){
        Objects.requireNonNull(postfix);
        return PREFIX + postfix;
    }
}
