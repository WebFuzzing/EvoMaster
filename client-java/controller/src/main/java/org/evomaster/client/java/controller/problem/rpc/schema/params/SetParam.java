package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * thrift
 *     HashSet (see https://thrift.apache.org/docs/types#containers)
 */
public class SetParam extends CollectionParam<Set<NamedTypedValue>>{

    public SetParam(String name, CollectionType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
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
        }).collect(Collectors.toSet());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        dto.type.type = RPCSupportedDataType.SET;
        if (getValue() != null){
            dto.innerContent = getValue().stream().map(s-> s.getDto()).collect(Collectors.toList());
        }
        return dto;
    }

    @Override
    public SetParam copyStructure() {
        return new SetParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!= null && !dto.innerContent.isEmpty()){
            NamedTypedValue t = getType().getTemplate();
            Set<NamedTypedValue> values = dto.innerContent.stream().map(s-> {
                NamedTypedValue v = t.copyStructureWithProperties();
                v.setValueBasedOnDto(s);
                return v;
            }).collect(Collectors.toSet());
            setValue(values);
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        NamedTypedValue t = getType().getTemplate();
        // employ linked hash set to avoid flaky tests
        Set<NamedTypedValue> values = new LinkedHashSet<>();
        for (Object e : (Set) instance){
            NamedTypedValue copy = t.copyStructureWithProperties();
            copy.setValueBasedOnInstance(e);
            values.add(copy);
        }
        setValue(values);
    }

    @Override
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException {

        Object instance = json;
        if (json instanceof String)
            instance = parseValueWithJson((String) json);

        if (instance == null){
            setValue(null); return;
        }

        if (!isValidInstance(instance))
            throw new RuntimeException("cannot parse Set param "+getName()+" with the type "+json.getClass().getName());

        NamedTypedValue t = getType().getTemplate();
        Set<NamedTypedValue> values = new LinkedHashSet<>();


        for (Object e : (Set) instance){
            NamedTypedValue copy = t.copyStructureWithProperties();
            copy.setValueBasedOnInstanceOrJson(e);
            values.add(copy);
        }
        setValue(values);


    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava) {
        String fullName = getType().getTypeNameForInstance();
        List<String> codes = new ArrayList<>();
        String var = CodeJavaOrKotlinGenerator.oneLineInstance(isDeclaration, doesIncludeName, fullName, variableName, null, );
        CodeJavaOrKotlinGenerator.addCode(codes, var, indent);
        if (getValue() == null) return codes;
        CodeJavaOrKotlinGenerator.addCode(codes, "{", indent);
        // new array
        CodeJavaOrKotlinGenerator.addCode(codes,
                CodeJavaOrKotlinGenerator.setInstance(
                        variableName,
                        CodeJavaOrKotlinGenerator.newSet()), indent+1);
        int index = 0;
        for (NamedTypedValue e: getValue()){
            String eVarName = CodeJavaOrKotlinGenerator.handleVariableName(variableName+"_e_"+index);
            codes.addAll(e.newInstanceWithJavaOrKotlin(true, true, eVarName, indent+1, ));
            CodeJavaOrKotlinGenerator.addCode(codes, variableName+".add("+eVarName+");", indent+1);
            index++;
        }

        CodeJavaOrKotlinGenerator.addCode(codes, "}", indent);
        return codes;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
        List<String> codes = new ArrayList<>();
        if (getValue() == null){
            CodeJavaOrKotlinGenerator.addCode(codes, CodeJavaOrKotlinGenerator.junitAssertNull(responseVarName), indent);
            return codes;
        }
        CodeJavaOrKotlinGenerator.addCode(codes, CodeJavaOrKotlinGenerator.junitAssertEquals(""+getValue().size(), CodeJavaOrKotlinGenerator.withSize(responseVarName)), indent);
        /*
            it is tricky to check values for set since the sequence is not determinate
         */
        return codes;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }
}
