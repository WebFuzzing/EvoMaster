package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.*;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

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

        if (!isValidInstance(instance)
            && !Collection.class.isAssignableFrom(instance.getClass()) // jackson might get list of json object
        )
            throw new RuntimeException("cannot parse Set param "+getName()+" with the type "+json.getClass().getName());



        NamedTypedValue t = getType().getTemplate();
        Set<NamedTypedValue> values = new LinkedHashSet<>();


        for (Object e : (Collection) instance){
            NamedTypedValue copy = t.copyStructureWithProperties();
            copy.setValueBasedOnInstanceOrJson(e);
            values.add(copy);
        }
        setValue(values);


    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        String fullName = getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        List<String> codes = new ArrayList<>();
        addCode(codes, oneLineInstance(isDeclaration, doesIncludeName, fullName, variableName, null, isJava, isNullable()), indent);
        if (getValue() == null) return codes;
        addCode(codes, codeBlockStart(isJava), indent);
        // new set
        addCode(codes,
                setInstance(
                        variableName,
                        newSet(isJava, getType().getTemplate().getType().getTypeNameForInstanceInJavaOrKotlin(isJava)), isJava), indent+1);
        int index = 0;
        for (NamedTypedValue e: getValue()){
            String eVarName = handleVariableName(variableName+"_e_"+index);
            codes.addAll(e.newInstanceWithJavaOrKotlin(true, true, eVarName, indent+1, isJava, false));
            addCode(codes, methodInvocation(variableName, "add", eVarName, isJava, isNullable(), false) + getStatementLast(isJava), indent+1);
            index++;
        }

        addCode(codes, "}", indent);
        return codes;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        List<String> codes = new ArrayList<>();
        if (getValue() == null){
            addCode(codes, junitAssertNull(responseVarName, isJava), indent);
            return codes;
        }
        addCode(codes, junitAssertEquals(String.valueOf(getValue().size()), CodeJavaOrKotlinGenerator.withSizeInAssertion(responseVarName, isJava, isNullable()),isJava ), indent);
        /*
            it is tricky to check values for set since the sequence is not determinate
         */
        return codes;
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return null;
    }
}
