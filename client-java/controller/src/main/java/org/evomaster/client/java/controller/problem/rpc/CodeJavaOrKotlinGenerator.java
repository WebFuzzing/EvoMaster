package org.evomaster.client.java.controller.problem.rpc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * a set of util for generating instance creation and assertion with java in tests
 * in order to make endpoint invocation
 */
public class CodeJavaOrKotlinGenerator {

    /**
     * a null expression in java
     */
    private final static String NULL_EXP = "null";

    /**
     * a method in SutHandler in order to get RPC client
     */
    private final static String GET_CLIENT_METHOD = "getRPCClient";


    private final static String KOTLIN_DECLARATION_VARIABLE = "var";
    private final static String KOTLIN_DECLARATION_VARIABLE_NULLABLE = "?";

    private final static String KOTLIN_NON_NULL_ASSERTED = "!!";

    private final static String JAVA_LANG = "java.lang.";

    private final static String KOTLIN_DEFAULT = "kotlin.";

    /**
     * representation of class with generic
     */
    public static String handleClassNameWithGeneric(String fullName, List<String> genericTypes){
        if (genericTypes == null || genericTypes.isEmpty()) return fullName;
        return String.format("%s<%s>", fullName, String.join(", ", formatBasicTypes(genericTypes)));
    }


    private static List<String> formatBasicTypes(List<String> types){
        return types.stream().map(CodeJavaOrKotlinGenerator::formateBasicType).collect(Collectors.toList());
    }

    private static String formateBasicType(String type){
        if (!type.startsWith(JAVA_LANG) && !type.startsWith(KOTLIN_DEFAULT)) return type;
        String[] pars = type.split("\\.");
        return pars[pars.length -1];
    }

    /**
     * handle escape char in string in java
     * https://docs.oracle.com/javase/tutorial/java/data/characters.html
     *
     * @param orgValue is an original string
     * @param isJava
     * @return string with handled escape char
     */
    public static String handleEscapeCharInString(String orgValue, boolean isJava){
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
                case '$':{
                    if (!isJava){
                        sb.append("\\$");
                        break;
                    }
                }
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }


    public static String typeNameOfArrayOrCollection(String collectionType, boolean isArray, String genericType, boolean isJava){
        if (isArray){
            if (isJava)
                return String.format("%s[]", genericType);
            else
                return String.format("%s<%s>", collectionType, genericType);
        }

        return String.format("%s<%s>", collectionType, genericType);

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
     *
     * @param isDeclaration   whether the instance is also for declaration
     * @param doesIncludeName whether to include variable name
     * @param fullName        is the full name of the variable
     * @param varName         is the variable name
     * @param value           is string to create the instance
     * @param isJava
     * @param isNullable
     * @return a string which could create the instance
     */
    public static String oneLineInstance(boolean isDeclaration, boolean doesIncludeName, String fullName, String varName, String value, boolean isJava, boolean isNullable){
        return oneLineInstance(isDeclaration, doesIncludeName, fullName, varName, value, false, isJava, isNullable);
    }

    /**
     * create an instance with one line
     * eg, fullName varName = value;
     *
     * @param isDeclaration   whether the instance is also for declaration
     * @param doesIncludeName whether to include variable name
     * @param fullName        is the full name of the variable
     * @param varName         is the variable name
     * @param value           is string to create the instance
     * @param isPrimitive     indicates whether it is primitive type
     * @param isJava
     * @param isNullable
     * @return a string which could create the instance
     */
    public static String oneLineInstance(boolean isDeclaration, boolean doesIncludeName, String fullName, String varName, String value, Boolean isPrimitive, boolean isJava, boolean isNullable){
        StringBuilder sb = new StringBuilder();
        if (isDeclaration){
            if (isJava)
                sb.append(handleNestedSymbolInTypeName(fullName));
            else
                sb.append(KOTLIN_DECLARATION_VARIABLE);
            sb.append(" ");
        }

        if (doesIncludeName){
            sb.append(varName);
            if (!isJava && isDeclaration){
                String afterType = "";
                if (isNullable)
                    afterType = KOTLIN_DECLARATION_VARIABLE_NULLABLE;
                if (fullName !=null)
                    sb.append(String.format(": %s%s", handleNestedSymbolInTypeName(fullName), afterType));
            }
        }

        String stringValue = "";
        if (!isPrimitive && (isJava || isNullable)){
            stringValue = NULL_EXP;
        }

        if (value != null)
            stringValue = value;

        if (stringValue.length() > 0)
            sb.append(" = ").append(stringValue);

        sb.append(getStatementLast(isJava));

        return sb.toString();
    }

    /**
     * set instance with setter
     * eg, varName.setterMethodName((fullName)value)
     *
     * @param setterMethodName is the setter method name
     * @param fullName         is full name of the instance
     * @param varName          is variable name
     * @param value            of the instance
     * @param isJava
     * @param isNullable
     * @return a string which set instance
     */
    public static String oneLineSetterInstance(String setterMethodName, String fullName, String varName, String value, boolean isJava, boolean isNullable){
        String stringValue = NULL_EXP;
        if (value != null)
            stringValue = castToType(fullName, value, isJava);

        return String.format("%s%s.%s(%s)%s", varName, variableNullableMark(isJava, isNullable),setterMethodName, stringValue, getStatementLast(isJava));
    }

    /**
     * process [varName] = new Object()
     *
     * @param fullName specifies the full name of the class
     * @param varName  specifies the variable name
     * @param isJava
     * @return code to set value with new object which has default constructor
     */
    public static String setInstanceObject(String fullName, String varName, boolean isJava){

        return String.format("%s = %s%s", varName, newObject(fullName, isJava), getStatementLast(isJava));
    }

    /**
     * handle last of statement for Java or kotlin
     * @return eg, a string which appends ; at last for Java
     */
    public static String getStatementLast(boolean isJava){
        if (isJava)
            return ";";
        return "";
    }

    /**
     * @param fullName       is the full name of the dto
     * @param varBuilderName is the variable name for the builder
     * @param isJava
     * @return code to instantiate builder for proto3 dto
     */
    public static String newBuilderProto3(String fullName, String varBuilderName, boolean isJava){
        String dec = String.format("%s.Builder", fullName);
        String afterVar = "";
        if (!isJava){
            dec =  KOTLIN_DECLARATION_VARIABLE;
            afterVar = String.format(":%s%s", String.format("%s.Builder", fullName), KOTLIN_DECLARATION_VARIABLE_NULLABLE);
        }

        return String.format("%s %s%s = %s.newBuilder()%s", dec, varBuilderName, afterVar, fullName, getStatementLast(isJava));
    }

    private static String variableNullableMark(boolean isJava, boolean isNullable){
        if (isJava || !isNullable) return "";
        return KOTLIN_DECLARATION_VARIABLE_NULLABLE;
    }

    private static String variableNonNullAssertedMark(boolean isJava, boolean isNullable){
        if (isJava || !isNullable) return "";
        return KOTLIN_NON_NULL_ASSERTED;
    }

    /**
     * process [varName] = [instance]
     *
     * @param varName  specifies the variable name
     * @param instance specifies the instance
     * @param isJava
     * @return code to set value
     */
    public static String setInstance(String varName, String instance, boolean isJava){
        return String.format("%s = %s%s", varName, instance,getStatementLast(isJava));
    }

    /**
     * process [varName] = [instance]; or [instance];
     *
     * @param includeVarName specifies whether to set instance to variable
     * @param varName        specifies the variable name
     * @param instance       specifies the instance
     * @param isJava
     * @return code to set value
     */
    public static String setInstance(boolean includeVarName, String varName, String instance, boolean isJava){
        if (includeVarName)
            return String.format("%s = %s%s", varName, instance,getStatementLast(isJava));
        return instance + getStatementLast(isJava);
    }

    /**
     * process new Object()
     *
     * @param fullName is a full name of the type of object
     * @param isJava
     * @return code to new object with default constructor
     */
    private static String newObject(String fullName, boolean isJava){
        return newObjectConsParams(fullName, "", isJava);
    }

    /**
     * process new Object(p1, p2)
     *
     * @param fullName is a full name of the type of object
     * @param params   is a string which could represent a list of params divided with ,
     * @param isJava
     * @return code to new object with params constructor
     */
    public static String newObjectConsParams(String fullName, String params, boolean isJava){
        if (isJava)
            return String.format("new %s(%s)", handleNestedSymbolInTypeName(fullName), params);
        else
            return String.format("%s(%s)", handleNestedSymbolInTypeName(fullName), params);
    }

    /**
     * @param fullName is full name of the type
     * @param length   is length of array to new
     * @param isJava
     * @return a string which could create a new array, eg, new String[5]
     */
    public static String newArray(String fullName, int length, boolean isJava){
        if (isJava)
            return String.format("new %s[%d]", fullName, length);
        else
            return String.format("Array<%s>(%d)", fullName, length);
    }

    /**
     * @return a string which could create a new HashSet
     */
    public static String newSet(boolean isJava, String genericType){
        if (!isJava)
            return String.format("mutableSetOf<%s>()", genericType);
        return String.format("new %s<>()", HashSet.class.getName());
    }
    /**
     * @return a string which could create a new HashMap
     */
    public static String newMap(boolean isJava, String keyGenericType, String valueGenericType){
        if (!isJava)
            return String.format("mutableMapOf<%s, %s>()", keyGenericType, valueGenericType);
        return String.format("new %s<>()", HashMap.class.getName());
    }
    /**
     * @return a string which could create a new ArrayList
     */
    public static String newList(boolean isJava, String genericType){
        if (!isJava)
            return String.format("mutableListOf<%s>()", genericType);
        return String.format("new %s<>()", ArrayList.class.getName());
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
        codes.replaceAll(s-> CodeJavaOrKotlinGenerator.getIndent(indent)+s);
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

    public static String codeBlockStart(boolean isJava){
        if (isJava) return "{";
        return "run {";
    }

    public static String codeBlockEnd(boolean isJava){
        return "}";
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
     *
     * @param typeName to cast
     * @param objCode  is the code representing object to cast
     * @param isJava
     * @return a java code which casts obj to a type
     */
    public static String castToType(String typeName, String objCode, boolean isJava){
        if (typeName == null) return objCode;
        if (isJava)
            return String.format("((%s)(%s))", handleNestedSymbolInTypeName(typeName), objCode);
        else
            return String.format("%s as? %s?", objCode, handleNestedSymbolInTypeName(typeName));
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
     * @param obj            specifies an object which owns the method. it is nullable
     * @param methodName     specifies a name of the method
     * @param params         specifies a list of params
     * @param isJava
     * @param objIsNullable
     * @param isNotNullAsserted
     * @return code to invoke the method
     */
    public static String methodInvocation(String obj, String methodName, String params, boolean isJava, boolean objIsNullable, boolean isNotNullAsserted){
        if (obj == null)
            return String.format("%s(%s)", methodName, params);

        String mark = variableNullableMark(isJava, objIsNullable);
        if (isNotNullAsserted)
            mark = variableNonNullAssertedMark(isJava, objIsNullable);
        return String.format("%s%s.%s(%s)", obj,mark, methodName, params);
    }


    public static String fieldAccess(String obj, String filedName, boolean isJava, boolean objIsNullable, boolean isNonNullAsserted){
        if (obj == null)
            return filedName;
        String mark = variableNullableMark(isJava, objIsNullable);
        if (isNonNullAsserted)
            mark = variableNonNullAssertedMark(isJava, objIsNullable);
        return String.format("%s%s.%s", obj,mark, filedName);
    }

    /**
     * @param expectedValue is the excepted value
     * @param variableName  is the variable name to be checked
     * @param isJava
     * @return an assertion for equal with junit
     */
    public static String junitAssertEquals(String expectedValue, String variableName, boolean isJava){
        String assertionScript= String.format("assertEquals(%s, %s)", expectedValue, variableName) + getStatementLast(isJava);
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
     * @param variableName is the variable name to be checked
     * @param isJava
     * @return a null assertion with junit
     */
    public static String junitAssertNull(String variableName, boolean isJava){
        String assertionScript= String.format("assertNull(%s)", variableName) + getStatementLast(isJava);
        if (AssertionsUtil.getAssertionsWithComment(assertionScript)){
            return "//" + assertionScript;
        }
        return assertionScript;
    }

    /**
     * @param variableName is the variable which has size() method
     * @param isJava
     * @param isNullable
     * @return a string to get size() of the variable
     */
    public static String withSizeInAssertion(String variableName, boolean isJava, boolean isNullable){
        String methodName = "size()";
        if (!isJava)
            methodName = "size";
        return String.format("%s%s.%s", variableName, variableNonNullAssertedMark(isJava, isNullable), methodName);
    }
    /**
     * @param variableName is the variable which has length field
     * @param isJava
     * @param isNullable
     * @return a string to get length of the variable
     */
    public static String withLengthInAssertion(String variableName, boolean isJava, boolean isNullable){
        return String.format("%s%s.length", variableName, variableNonNullAssertedMark(isJava, isNullable));
    }

    /**
     * junit does not support equal assertions for double type, then employ numbersMatch here
     *
     * @param expectedValue is the excepted value
     * @param variableName  is the variable name to be checked
     * @param isJava
     * @return a True assertion with numbersMatch method.
     */
    public static String junitAssertNumbersMatch(String expectedValue, String variableName, boolean isJava){
        String assertionScript= String.format("assertTrue(numbersMatch(%s, %s))", expectedValue, variableName) + getStatementLast(isJava);
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
