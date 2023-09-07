package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.Protobuf3ByteStringType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * this is for handling bytes in gRPC with protobuf3 <a href="https://protobuf.dev/reference/java/api-docs/com/google/protobuf/ByteString">ByteString</a>
 *
 * handle it as string with utf8 now
 * <a href="https://protobuf.dev/reference/java/api-docs/com/google/protobuf/ByteString.html#copyFromUtf8-java.lang.String-">copyFromUtf8</a>
 * <a href="https://protobuf.dev/reference/java/api-docs/com/google/protobuf/ByteString.html#toStringUtf8--">toStringUtf8</a>
 */
public class Protobuf3ByteStringParam extends NamedTypedValue<Protobuf3ByteStringType,String>{


    public final static String PROTOBUF3_BYTE_STRING_METHOD_COPY_FROM_METHOD = "copyFromUtf8";

    public final static String PROTOBUF3_BYTE_STRING_METHOD_TO_STRING_UTF8 = "toStringUtf8";

    public Protobuf3ByteStringParam(String name, Protobuf3ByteStringType type, AccessibleSchema accessibleSchema){
        super(name, type, accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;

        try {
            Method setMethod = getType().getClazz().getMethod(PROTOBUF3_BYTE_STRING_METHOD_COPY_FROM_METHOD, String.class);
            Object instance = setMethod.invoke(null, getValue());
            return instance;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("fail to find method copyFromUtf8 in the class com.google.protobuf.ByteString with error msg:", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("fail to invoke method copyFromUtf8 in the class com.google.protobuf.ByteString with error msg:", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("fail to access method copyFromUtf8 in the class com.google.protobuf.ByteString with error msg:", e);
        }
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null){
            dto.stringValue = getValue();
        }

        return dto;
    }


    @Override
    public Protobuf3ByteStringParam copyStructure() {
        return new Protobuf3ByteStringParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.stringValue != null)
            setValue(dto.stringValue);
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        if (instance == null) setValue(null);
        try {
            Method getMethod = getType().getClazz().getMethod(PROTOBUF3_BYTE_STRING_METHOD_TO_STRING_UTF8);
            Object strutf8 = getMethod.invoke(instance);
            if (strutf8 instanceof String)
                setValue((String) strutf8);
            else{
                throw new RuntimeException("fail to get string value of ByteString with toStringUtf8");
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("fail to find method toStringUtf8 in the class com.google.protobuf.ByteString with error msg:", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("fail to invoke method toStringUtf8 in the class com.google.protobuf.ByteString with error msg:", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("fail to access method toStringUtf8 in the class com.google.protobuf.ByteString with error msg:", e);
        }

    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        List<String> codes = new ArrayList<>();
        String var = oneLineInstance(isDeclaration, doesIncludeName, getType().getFullTypeName(), variableName, null,isJava, isNullable());
        addCode(codes, var, indent);
        if (getValue() == null) return codes;
        addCode(codes, codeBlockStart(isJava), indent);
        addCode(codes,
            oneLineInstance(false, true, getType().getFullTypeName(), variableName, getType().getFullTypeName()+"."+PROTOBUF3_BYTE_STRING_METHOD_COPY_FROM_METHOD+"(\""+ getValue() + "\")",isJava, isNullable()), indent + 1);
        addCode(codes, codeBlockEnd(isJava), indent);
        return codes;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent));
        if (getValue() == null)
            sb.append(junitAssertNull(responseVarName, isJava));
        else{
            sb.append(junitAssertEquals("\""+getValueAsJavaString(isJava)+"\"", responseVarName+"."+PROTOBUF3_BYTE_STRING_METHOD_TO_STRING_UTF8+"()", isJava));
        }

        return Collections.singletonList(sb.toString());
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return getValue();
    }

    @Override
    public List<String> referenceTypes() {
        return null;
    }
}
