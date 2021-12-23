package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * array param
 */
public class ArrayParam extends CollectionParam<List<NamedTypedValue>>{

    public ArrayParam(String name, CollectionType type) {
        super(name, type);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        return getValue().stream().map(v-> {
            try {
                return v.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("ArrayParam: could not create new instance for value:"+v.getType());
            }
        }).toArray();
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.ARRAY;
        dto.type.example = getType().getTemplate().getDto();

        if (getValue() != null)
            dto.innerContent = getValue().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        return dto;
    }

    @Override
    public ArrayParam copyStructure() {
        return new ArrayParam(getName(), getType());
    }


    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (!dto.innerContent.isEmpty()){
            NamedTypedValue t = getType().getTemplate();
            List<NamedTypedValue> values = dto.innerContent.stream().map(s-> {
                NamedTypedValue v = t.copyStructure();
                v.setValueBasedOnDto(s);
                return v;
            }).collect(Collectors.toList());
            setValue(values);
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        NamedTypedValue t = getType().getTemplate();
        List<NamedTypedValue> values = new ArrayList<>();
        for (Object e : (Object[]) instance){
            NamedTypedValue copy = t.copyStructure();
            copy.setValueBasedOnInstance(e);
            values.add(copy);
        }
        setValue(values);
    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String fullName = getType().getTypeNameForInstance();
        List<String> codes = new ArrayList<>();
        String var = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, fullName, variableName, null);
        CodeJavaGenerator.addCode(codes, var, indent);
        if (getValue() == null) return codes;
        int length = getValue().size();
        CodeJavaGenerator.addCode(codes, "{", indent);
        // new array
        CodeJavaGenerator.addCode(codes,
                CodeJavaGenerator.setInstance(
                        variableName,
                        CodeJavaGenerator.newArray(getType().getTemplate().getType().getTypeNameForInstance(), length)), indent+1);
        int index = 0;
        for (NamedTypedValue e: getValue()){
            String eVar = variableName+"["+index+"]";
            codes.addAll(e.newInstanceWithJava(false, true, eVar, indent+1));
            index++;
        }

        CodeJavaGenerator.addCode(codes, "}", indent);
        return codes;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName) {
        List<String> codes = new ArrayList<>();
        if (getValue() == null){
            CodeJavaGenerator.addCode(codes, CodeJavaGenerator.junitAssertNull(responseVarName), indent);
            return codes;
        }
        CodeJavaGenerator.addCode(codes, CodeJavaGenerator.junitAssertEquals(""+getValue().size(), CodeJavaGenerator.withLength(responseVarName)), indent);

        int index = 0;
        for (NamedTypedValue e: getValue()){
            String eVar = responseVarName+"["+index+"]";
            codes.addAll(e.newAssertionWithJava(indent, eVar));
            index++;
        }
        return codes;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }
}
