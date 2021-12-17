package org.evomaster.client.java.controller.problem.rpc;

import java.util.*;
import java.util.stream.IntStream;

public class CodeJavaGenerator {

    private final static String NULL_EXP = "null";
    private final static String GET_CLIENT_METHOD = "getRPCClient";

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
        return String.format("new %s(%s)", fullName, params);
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
        return String.format("((%s)%s)", typeName, objCode);
    }

    /**
     * process the code to get RPC client
     * @param interfaceName specifies the interface name to get its corresponding client
     * @return code which enables getting RPC client
     */
    public static String getGetClientMethod(String interfaceName){
        return String.format("%s(%s)", GET_CLIENT_METHOD, interfaceName);
    }

    /**
     *
     * @param obj specifies an object which owns the method. it is nullable
     * @param methodName specifies a name of the method
     * @param params specifies a list of params
     * @return code to invoke the method
     */
    public static String methodInvocation(String obj, String methodName, String params){
        if (obj != null)
            return String.format("%s(%s)", methodName, params);

        return String.format("%s.%s(%s)", obj, methodName, params);
    }

    public static String appendLast(){
        return ";";
    }
}
