package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * thrift
 *  ArrayList (see https://thrift.apache.org/docs/types#containers)
 */
public class ListParam extends CollectionParam<List<NamedTypedValue>>{

    public ListParam(String name, CollectionType type) {
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
        }).collect(Collectors.toList());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.LIST;
        if (getValue() != null){
            dto.innerContent = getValue().stream().map(s-> s.getDto()).collect(Collectors.toList());
        }
        return dto;
    }

    @Override
    public ListParam copyStructure() {
        return new ListParam(getName(), getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!= null && !dto.innerContent.isEmpty()){
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
        for (Object e : (List) instance){
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
        CodeJavaGenerator.addCode(codes, "{", indent);
        // new array
        CodeJavaGenerator.addCode(codes,
                CodeJavaGenerator.setInstance(
                        variableName,
                        CodeJavaGenerator.newList()), indent+1);
        int index = 0;
        for (NamedTypedValue e: getValue()){
            String eVarName = variableName+"_e_"+index;
            codes.addAll(e.newInstanceWithJava(true, true, eVarName, indent+1));
            CodeJavaGenerator.addCode(codes, variableName+".add("+eVarName+");", indent+1);
            index++;
        }

        CodeJavaGenerator.addCode(codes, "}", indent);
        return codes;
    }

}
