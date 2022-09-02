package org.evomaster.client.java.instrumentation.shared;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaintInputName {

    /*
        WARNING:
        the naming here has to be kept in sync in ALL implementations of this class,
        including Java, JS and C#
     */

    private static final String PREFIX = "_EM_";

    private static final String POSTFIX = "_XYZ_";

    private static final Pattern pattern = Pattern.compile("(?i)\\Q"+PREFIX+"\\E\\d+\\Q"+POSTFIX+"\\E");

    /**
     * Name of special Query Param used by EM, to discover new params not in the schema (eg OpenAPI for REST),
     * based on what it is compared to
     */
    public static final String EXTRA_PARAM_TAINT = "EMextraParam123";


    /**
     * Name of special HTTP Header used by EM, to discover new headers not in the schema (eg OpenAPI for REST),
     * based on what it is compared to
     */
    public static final String EXTRA_HEADER_TAINT = "x-EMextraHeader123";


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

    /**
     * One problem when using this type of taint, is that there can be constraints on the length
     * of the strings... and the taint value might end up being longer than it :-(
     * Not sure if there is really any simple workaround... but hopefully should be
     * so rare that we can live with it
     */
    public static int getTaintNameMaxLength(){
        return PREFIX.length() + POSTFIX.length() + 6;
    }
}
