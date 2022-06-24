package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ByteBufferType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * this is created for handling binary in thrift, see https://thrift.apache.org/docs/types
 * handle it as string
 */
public class ByteBufferParam extends NamedTypedValue<ByteBufferType, ByteBuffer>{

    public ByteBufferParam(String name, AccessibleSchema accessibleSchema) {
        super(name, new ByteBufferType(), accessibleSchema);
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
        return new ByteBufferParam(getName(), accessibleSchema);
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
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        List<String> codes = new ArrayList<>();
        String var = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, ByteBuffer.class.getName(), variableName, null);
        CodeJavaGenerator.addCode(codes, var, indent);
        if (getValue() == null) return codes;
        CodeJavaGenerator.addCode(codes, "{", indent);
        String varValue = variableName+"_byteArray";
        String byteArray = "\""+ new String(getValue().array(), StandardCharsets.UTF_8) + "\".getBytes("+StandardCharsets.class.getName()+".UTF_8)";
        CodeJavaGenerator.addCode(codes,
                CodeJavaGenerator.oneLineInstance(true, true, "byte[]", varValue, byteArray), indent + 1);
        CodeJavaGenerator.addCode(codes,
                CodeJavaGenerator.oneLineInstance(false, true, String.class.getName(), variableName, ByteBuffer.class.getName()+".allocate("+varValue+".length)"), indent + 1);
        CodeJavaGenerator.addCode(codes, variableName+".put("+varValue+");", indent+1);
        CodeJavaGenerator.addCode(codes, "}", indent);

        return codes;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
        StringBuilder sb = new StringBuilder();
        sb.append(CodeJavaGenerator.getIndent(indent));
        if (getValue() == null)
            sb.append(CodeJavaGenerator.junitAssertNull(responseVarName));
        else
            sb.append("// not handle ByteBuffer assertion");
        return Collections.singletonList(sb.toString());
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }


}
