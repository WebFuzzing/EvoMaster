package org.evomaster.client.java.instrumentation.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaintInputName {

    private static final String PREFIX = "evomaster_";

    private static final String POSTFIX = "_input";

    private static final Pattern pattern = Pattern.compile("\\Q"+PREFIX+"\\E\\d+\\Q"+POSTFIX+"\\E");

    /**
     * Check if a given string value is a tainted value
     */
    public static boolean isTaintInput(String value){
        if(value == null){
            return false;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }


    public static boolean includesTaintInput(String value){
        if(value == null){
            return false;
        }
        Matcher matcher = pattern.matcher(value);
        return matcher.find();
    }


    /**
     * Create a tainted value, with the input id being part of it
     */
    public static String getTaintName(int id){
        if(id < 0){
            throw new IllegalArgumentException("Negative id");
        }
        /*
            Note: this is quite simple, we simply add a unique prefix
            and postfix, in lowercase.
            But we would not be able to check if the part of the id was
            modified.
         */
        return PREFIX + id + POSTFIX;
    }
}
