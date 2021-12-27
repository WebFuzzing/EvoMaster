package org.evomaster.client.java.controller.problem.rpc;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class CodeJavaGenerator {

    private final static String NULL_EXP = "null";
    private final static String GET_CLIENT_METHOD = "getRPCClient";

    /**
     * handle escape char in string in java
     *      https://docs.oracle.com/javase/tutorial/java/data/characters.html
     * @param orgValue is an original string
     * @return string with handled escape char
     */
    public static String handleEscapeCharInString(String orgValue){
        StringBuilder sb = new StringBuilder();
        for (char c: orgValue.toCharArray()){
            switch (c){
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\f': sb.append("\\f"); break;
                case '\'': sb.append("\\'"); break;
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String enumValue(String enumTypeName, String itemName){
        return String.format("%s.%s", handleNestedSymbolInTypeName(enumTypeName), itemName);
    }

    public static String oneLineInstance(boolean isDeclaration, boolean doesIncludeName, String fullName, String varName, String value){
        StringBuilder sb = new StringBuilder();
        if (isDeclaration)
            sb.append(handleNestedSymbolInTypeName(fullName)).append(" ");
        if (doesIncludeName)
            sb.append(varName).append(" = ");
        String stringValue = NULL_EXP;
        if (value != null)
            stringValue = value;
        sb.append(stringValue).append(";");
        return sb.toString();
    }

    /**
     * process [varName] = new Object()
     * @param varName specifies the variable name
     * @param fullName specifies the full name of the class
     * @return code to set value with new object which has default constructor
     */
    public static String setInstanceObject(String fullName, String varName){
        return String.format("%s = %s;", varName, newObject(fullName));
    }

    /**
     * process [varName] = [instance]
     * @param varName specifies the variable name
     * @param instance specifies the instance
     * @return code to set value
     */
    public static String setInstance(String varName, String instance){
        return String.format("%s = %s;", varName, instance);
    }

    /**
     * process [varName] = [instance]; or [instance];
     * @param includeVarName specifies whether to set instance to variable
     * @param varName specifies the variable name
     * @param instance specifies the instance
     * @return code to set value
     */
    public static String setInstance(boolean includeVarName, String varName, String instance){
        if (includeVarName)
            return String.format("%s = %s;", varName, instance);
        return instance+ appendLast();
    }

    /**
     * process new Object()
     * @param fullName is a full name of the type of object
     * @return code to new object with default constructor
     */
    public static String newObject(String fullName){
        return newObjectConsParams(fullName, "");
    }

    /**
     * process new Object(p1, p2)
     * @param fullName is a full name of the type of object
     * @param params is a string which could represent a list of params divided with ,
     * @return code to new object with params constructor
     */
    public static String newObjectConsParams(String fullName, String params){
        return String.format("new %s(%s)", handleNestedSymbolInTypeName(fullName), params);
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

    public static List<String> addComment(List<String> codes, String comment, int indent){
        codes.add(getIndent(indent) + "// " + comment);
        return codes;
    }

    /**
     * cast object to a type
     * @param typeName to cast
     * @param objCode is the code representing object to cast
     * @return a java code which casts obj to a type
     */
    public static String castToType(String typeName, String objCode){
        return String.format("((%s)%s)", handleNestedSymbolInTypeName(typeName), objCode);
    }

    private static String handleNestedSymbolInTypeName(String typeName){
        return typeName.replaceAll("\\$","\\.");
    }

    /**
     * process the code to get RPC client
     * @param controllerVarName specifies the controller variable name
     * @param interfaceName specifies the interface name to get its corresponding client
     * @return code which enables getting RPC client
     */
    public static String getGetClientMethod(String controllerVarName, String interfaceName){
        return String.format("%s(%s)", controllerVarName + "." + GET_CLIENT_METHOD, interfaceName);
    }

    /**
     *
     * @param obj specifies an object which owns the method. it is nullable
     * @param methodName specifies a name of the method
     * @param params specifies a list of params
     * @return code to invoke the method
     */
    public static String methodInvocation(String obj, String methodName, String params){
        if (obj == null)
            return String.format("%s(%s)", methodName, params);

        return String.format("%s.%s(%s)", obj, methodName, params);
    }

    public static String appendLast(){
        return ";";
    }

    public static String junitAssertEquals(String expectedValue, String variableName){
        return String.format("assertEquals(%s, %s)", expectedValue, variableName) + appendLast();
    }

    public static String junitAssertNull(String variableName){
        return String.format("assertNull(%s)", variableName) + appendLast();
    }

    public static String withSize(String variableName){
        return String.format("%s.size()", variableName);
    }

    public static String withLength(String variableName){
        return String.format("%s.length", variableName);
    }

    public static String junitAssertNumbersMatch(String expectedValue, String variableName){
        return String.format("assertTrue(numbersMatch(%s, %s))", expectedValue, variableName) + appendLast();
    }

    public static String handleVariableName(String original){
        return original.replaceAll("\\.","_");
    }
}
