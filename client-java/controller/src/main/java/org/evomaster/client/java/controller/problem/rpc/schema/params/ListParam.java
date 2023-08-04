package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * thrift
 *  ArrayList (see https://thrift.apache.org/docs/types#containers)
 */
public class ListParam extends CollectionParam<List<NamedTypedValue>>{

    public ListParam(String name, CollectionType type, AccessibleSchema accessibleSchema) {
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
        return new ListParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!= null && !dto.innerContent.isEmpty()){
            NamedTypedValue t = getType().getTemplate();
            List<NamedTypedValue> values = dto.innerContent.stream().map(s-> {
                NamedTypedValue v = t.copyStructureWithProperties();
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
            throw new RuntimeException("cannot parse List param "+getName()+" with the type "+json.getClass().getName());

        NamedTypedValue t = getType().getTemplate();
        List<NamedTypedValue> values = new ArrayList<>();

        for (Object e : (List) instance){
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
                        CodeJavaOrKotlinGenerator.newList()), indent+1);
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

        if (maxAssertionForDataInCollection == 0)
            return codes;

        List<Integer> nvalue = null;
        if (maxAssertionForDataInCollection > 0 && getValue().size() > maxAssertionForDataInCollection){
            nvalue = CodeJavaOrKotlinGenerator.randomNInt(getValue().size(), maxAssertionForDataInCollection);
        }else
            nvalue = IntStream.range(0, getValue().size()).boxed().collect(Collectors.toList());

        for (int index : nvalue){
            NamedTypedValue e = getValue().get(index);
            String eVar = responseVarName+".get("+index+")";
            codes.addAll(e.newAssertionWithJava(indent, eVar, maxAssertionForDataInCollection));
        }

        return codes;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }

}
