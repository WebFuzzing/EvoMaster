package org.evomaster.client.java.controller.problem.rpc;

import java.util.*;
import java.util.stream.IntStream;

public class CodeJavaGenerator {

    private final static String NULL_EXP = "null";

    public static String oneLineInstance(boolean isDeclaration, boolean doesIncludeName, String fullName, String varName, String value){
        StringBuilder sb = new StringBuilder();
        if (isDeclaration)
            sb.append(fullName).append(" ");
        if (doesIncludeName)
            sb.append(varName).append(" = ");
        String stringValue = NULL_EXP;
        if (value != null)
            stringValue = value;
        sb.append(stringValue).append(";");
        return sb.toString();
    }


    public static String setInstanceObject(String fullName, String varName){
        return String.format("%s = %s;", varName, newObject(fullName));
    }


    public static String setInstance(String varName, String instance){
        return String.format("%s = %s;", varName, instance);
    }


    public static String newObject(String fullName){
        return String.format("new %s()", fullName);
    }

    public static String newArray(String fullName, int length){
        return String.format("new %s[%d]", fullName, length);
    }

    public static String newSet(){
        return "new "+ HashSet.class.getName()+"<>()";
    }

    public static String newMap(){
        return "new " + HashMap.class.getName()+"<>()";
    }

    public static String newList(){
        return "new "+ArrayList.class.getName()+"<>()";
    }

    public static String getIndent(int indent){
        return String.join("", Collections.nCopies(indent, " "));
    }

    public static List<String> getStringListWithIndent(List<String> codes, int indent){
        codes.replaceAll(s-> CodeJavaGenerator.getIndent(indent)+s);
        return codes;
    }

    public static List<String> addCode(List<String> codes, String code, int indent){
        codes.add(getIndent(indent) + code);
        return codes;
    }
}
