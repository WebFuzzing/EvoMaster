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
     * Unique identified to specify a map has been tainted by EvoMaster.
     * This identifier will be used as a key-name for a field contained a tainted string identifying the map.
     * If at any point in time we operate on a map, and such map has this key, then we know we need to handle
     * it with taint analysis.
     */
    public static final String TAINTED_MAP_EM_LABEL_IDENTIFIER = "EM_tainted_map";


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
        return getTaintName(id,0);
    }

    public static String getTaintName(int id, int minLength){
        if(id < 0){
            throw new IllegalArgumentException("Negative id");
        }
        if(minLength < 0){
            throw new IllegalArgumentException("Negative minLength");
        }
        /*
            Note: this is quite simple, we simply add a unique prefix
            and postfix, in lowercase.
            But we would not be able to check if the part of the id was
            modified.
         */

        String s = "" + id;
        String taint =  PREFIX + s + POSTFIX;
        if(taint.length() < minLength){
            //need padding
            int diff = minLength - taint.length();
            taint = PREFIX + s + new String(new char[diff]).replace("\0", "0") + POSTFIX;
        }
        return taint;
    }

    /**
     * One problem when using this type of taint, is that there can be constraints on the length
     * of the strings... and the taint value might end up being longer than it :-(
     * Not sure if there is really any simple workaround... but hopefully should be
     * so rare that we can live with it
     */
    public static boolean doesTaintNameSatisfiesLengthConstraints(String id, int maxLength){
        return (PREFIX.length() + POSTFIX.length() + id.length()) <= maxLength;
    }
}
