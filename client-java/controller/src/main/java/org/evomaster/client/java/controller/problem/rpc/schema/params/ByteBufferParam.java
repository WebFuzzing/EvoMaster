package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ByteBufferType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * this is created for handling binary in thrift, see https://thrift.apache.org/docs/types
 * handle it as string
 */
public class ByteBufferParam extends NamedTypedValue<ByteBufferType, ByteBuffer>{

    public ByteBufferParam(String name, AccessibleSchema accessibleSchema, JavaDtoSpec spec) {
        super(name, new ByteBufferType(spec), accessibleSchema);
    }

    public void setValue(byte[] value) {
        ByteBuffer buffer = ByteBuffer.allocate(value.length);
        buffer.put(value);
        this.setValue(buffer);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        return getValue();
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null){
            // bytebuffer is now handled as string
            dto.stringValue = new String(getValue().array(), StandardCharsets.UTF_8);
        }

        return dto;
    }

    @Override
    public ByteBufferParam copyStructure() {
        return new ByteBufferParam(getName(), accessibleSchema, this.getType().spec);
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.stringValue != null)
            setValue(dto.stringValue.getBytes());
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        setValue((ByteBuffer) instance);
    }

    @Override
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException {
        if (json == null)  return;
        if (!(json instanceof String))
            throw new IllegalArgumentException("Cannot set value for ByteBufferParam with the type:"+json.getClass().getName());
        setValue(((String)json).getBytes());
    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        List<String> codes = new ArrayList<>();
        String var = oneLineInstance(isDeclaration, doesIncludeName, ByteBuffer.class.getName(), variableName, null,isJava,isNullable() );
        addCode(codes, var, indent);
        if (getValue() == null) return codes;
        addCode(codes, codeBlockStart(isJava), indent);
        String varValue = variableName+"_byteArray";
        String byteArray = methodInvocation("\""+ new String(getValue().array(), StandardCharsets.UTF_8)+ "\"", isJava?"getBytes":"toByteArray", StandardCharsets.class.getName()+".UTF_8", isJava, false, false);
        addCode(codes,
                oneLineInstance(true, true, isJava?"byte[]":"ByteArray", varValue, byteArray, isJava,isNullable() ), indent + 1);
        addCode(codes,
                oneLineInstance(false, true, "String", variableName, ByteBuffer.class.getName()+".allocate("+
                        fieldAccess(varValue, (isJava?"length":"size")+")", isJava, isNullable(), true), isJava,isNullable() ), indent + 1);
        addCode(codes,
            methodInvocation(variableName, "put", varValue, isJava, isNullable(), false)+ getStatementLast(isJava),
            indent+1);
        addCode(codes, codeBlockEnd(isJava), indent);

        return codes;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent));
        if (getValue() == null)
            sb.append(junitAssertNull(responseVarName, isJava));
        else
            sb.append("// not handle ByteBuffer assertion");
        return Collections.singletonList(sb.toString());
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return null;
    }

    @Override
    public List<String> referenceTypes() {
        return null;
    }
}
