package org.evomaster.client.java.controller.problem.rpc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a set of util for generating instance creation and assertion with java in tests
 * in order to make endpoint invocation
 */
public class CodeJavaGenerator {

    /**
     * a null expression in java
     */
    private final static String NULL_EXP = "null";

    /**
     * a method in SutHandler in order to get RPC client
     */
    private final static String GET_CLIENT_METHOD = "getRPCClient";


    public static String handleClassNameWithGeneric(String fullName, List<String> genericTypes){
        if (genericTypes == null || genericTypes.isEmpty()) return fullName;
        return String.format("%s<%s>", fullName, String.join(", ", genericTypes));
    }

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

    /**
     *
     * @param enumTypeName the enum name
     * @param itemName the item name of the enum
     * @return a string which could retrieve the item of a Enum type, eg, Gender.Female
     */
    public static String enumValue(String enumTypeName, String itemName){
        return String.format("%s.%s", handleNestedSymbolInTypeName(enumTypeName), itemName);
    }

    /**
     * create an instance with one line
     * eg, fullName varName = value;
     * @param isDeclaration whether the instance is also for declaration
     * @param doesIncludeName whether to include variable name
     * @param fullName is the full name of the variable
     * @param varName is the variable name
     * @param value is string to create the instance
     * @return a string which could create the instance
     */
    public static String oneLineInstance(boolean isDeclaration, boolean doesIncludeName, String fullName, String varName, String value){
        return oneLineInstance(isDeclaration, doesIncludeName, fullName, varName, value, false);
    }

    /**
     * create an instance with one line
     * eg, fullName varName = value;
     * @param isDeclaration whether the instance is also for declaration
     * @param doesIncludeName whether to include variable name
     * @param fullName is the full name of the variable
     * @param varName is the variable name
     * @param value is string to create the instance
     * @param isPrimitive indicates whether it is primitive type
     * @return a string which could create the instance
     */
    public static String oneLineInstance(boolean isDeclaration, boolean doesIncludeName, String fullName, String varName, String value, Boolean isPrimitive){
        StringBuilder sb = new StringBuilder();
        if (isDeclaration)
            sb.append(handleNestedSymbolInTypeName(fullName)).append(" ");
        if (doesIncludeName){
            sb.append(varName);
            if (value != null || !isPrimitive)
                sb.append(" = ");
        }
        String stringValue = NULL_EXP;
        if (isPrimitive)
            stringValue = "";
        if (value != null)
            stringValue = value;

        sb.append(stringValue).append(";");
        return sb.toString();
    }

    /**
     * set instance with setter
     * eg, varName.setterMethodName((fullName)value)
     * @param setterMethodName is the setter method name
     * @param fullName is full name of the instance
     * @param varName is variable name
     * @param value of the instance
     * @return a string which set instance
     */
    public static String oneLineSetterInstance(String setterMethodName, String fullName, String varName, String value){
        String stringValue = NULL_EXP;
        if (value != null)
            stringValue = castToType(fullName, value);

        return String.format("%s.%s(%s);", varName, setterMethodName, stringValue);
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

    /**
     *
     * @param fullName is full name of the type
     * @param length is length of array to new
     * @return a string which could create a new array, eg, new String[5]
     */
    public static String newArray(String fullName, int length){
        return String.format("new %s[%d]", fullName, length);
    }

    /**
     * @return a string which could create a new HashSet
     */
    public static String newSet(){
        return "new "+ HashSet.class.getName()+"<>()";
    }
    /**
     * @return a string which could create a new HashMap
     */
    public static String newMap(){
        return "new " + HashMap.class.getName()+"<>()";
    }
    /**
     * @return a string which could create a new ArrayList
     */
    public static String newList(){
        return "new "+ArrayList.class.getName()+"<>()";
    }

    /**
     * @param indent is a number of indent (here we use space)
     * @return a string which contains [indent] space
     */
    public static String getIndent(int indent){
        return String.join("", Collections.nCopies(indent, " "));
    }

    /**
     *
     * @param codes a list of codes, could be a block
     * @param indent is a number of indent for the codes
     * @return a list of indented codes
     */
    public static List<String> getStringListWithIndent(List<String> codes, int indent){
        codes.replaceAll(s-> CodeJavaGenerator.getIndent(indent)+s);
        return codes;
    }

    /**
     *
     * @param codes is a list of current code
     * @param code is one line code to be appended to the [codes]
     * @param indent is a number of indent for the [code]
     * @return a list of codes appended with the [code]
     */
    public static List<String> addCode(List<String> codes, String code, int indent){
        codes.add(getIndent(indent) + code);
        return codes;
    }

    /**
     *
     * @param codes is a list of current code
     * @param comment is one line comment
     * @param indent is a number of indent for the [code]
     * @return a list of codes with comment
     */
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
        if (typeName == null) return objCode;
        return String.format("((%s)(%s))", handleNestedSymbolInTypeName(typeName), objCode);
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

    /**
     * handle last for java
     * @return a string which appends ; at last
     */
    public static String appendLast(){
        return ";";
    }

    /**
     *
     * @param expectedValue is the excepted value
     * @param variableName is the variable name to be checked
     * @return an assertion for equal with junit
     */
    public static String junitAssertEquals(String expectedValue, String variableName){
        String assertionScript= String.format("assertEquals(%s, %s)", expectedValue, variableName) + appendLast();
        if (AssertionsUtil.getAssertionsWithComment(assertionScript)){
            return "//" + assertionScript;
        }

        /*
            if length of expected value is more than 10, comment out this assertion for the moment

            TODO need to better handle flakiness later

            https://trello.com/c/v5XRNyml/707-flakiness-configurable-fields-to-skip
         */
        if (expectedValue.length() > 10)
            return "//" + assertionScript;

        return assertionScript;
    }

    /**
     *
     * @param variableName is the variable name to be checked
     * @return a null assertion with junit
     */
    public static String junitAssertNull(String variableName){
        String assertionScript= String.format("assertNull(%s)", variableName) + appendLast();
        if (AssertionsUtil.getAssertionsWithComment(assertionScript)){
            return "//" + assertionScript;
        }
        return assertionScript;
    }

    /**
     *
     * @param variableName is the variable which has size() method
     * @return a string to get size() of the variable
     */
    public static String withSize(String variableName){
        return String.format("%s.size()", variableName);
    }
    /**
     *
     * @param variableName is the variable which has length field
     * @return a string to get length of the variable
     */
    public static String withLength(String variableName){
        return String.format("%s.length", variableName);
    }

    /**
     * junit does not support equal assertions for double type, then employ numbersMatch here
     * @param expectedValue is the excepted value
     * @param variableName is the variable name to be checked
     * @return a True assertion with numbersMatch method.
     */
    public static String junitAssertNumbersMatch(String expectedValue, String variableName){
        String assertionScript= String.format("assertTrue(numbersMatch(%s, %s))", expectedValue, variableName) + appendLast();
        if (AssertionsUtil.getAssertionsWithComment(assertionScript)){
            return "//" + assertionScript;
        }
        return assertionScript;
    }

    /**
     * some variable name might contain invalid symbol such as ., then need to reformat them
     * @param original is the original name of the variable
     * @return a reformatted variable name
     */
    public static String handleVariableName(String original){
        return original.replaceAll("\\.","_");
    }


    /**
     * select n int from a range [0, max)
     * Note that this is only used for deciding a number of data in collection to be asserted
     * then it should not have any side-effect to randomness of search
     * @param max
     * @param n
     * @return
     */
    public static List<Integer> randomNInt(int max, int n){
        if (n >= max){
            throw new IllegalStateException("count should be less than max");
        }
        List<Integer> candidates = IntStream.range(0, max).boxed().collect(Collectors.toList());
        Collections.shuffle(candidates);
        return  candidates.subList(0, n);
    }
}
