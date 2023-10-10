package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PairType;

import java.util.*;

/**
 * map entry which is only used for handling map
 */
public class PairParam extends NamedTypedValue<PairType, AbstractMap.SimpleEntry<NamedTypedValue, NamedTypedValue>>{
    public final static String PAIR_NAME = "MAP_ENTRY";

    public PairParam(PairType type, AccessibleSchema accessibleSchema) {
        super(PAIR_NAME, type, accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        return new AbstractMap.SimpleEntry<>(getValue().getKey().newInstance(), getValue().getKey().newInstance());
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null)
            dto.innerContent = Arrays.asList(getValue().getKey().getDto(), getValue().getValue().getDto());
        return dto;
    }

    @Override
    public PairParam copyStructure() {
        return new PairParam(getType(), accessibleSchema);
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent.size() == 2){
            NamedTypedValue first = getType().getFirstTemplate().copyStructureWithProperties();
            NamedTypedValue second = getType().getSecondTemplate().copyStructureWithProperties();
            first.setValueBasedOnDto(dto.innerContent.get(0));
            second.setValueBasedOnDto(dto.innerContent.get(1));
            setValue(new AbstractMap.SimpleEntry(first, second));
        } else
            throw new RuntimeException("ERROR: size of inner content of dto is not 2 for pair type, i.e., "+ dto.innerContent.size());
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        if (instance == null) return;
        NamedTypedValue first = getType().getFirstTemplate().copyStructureWithProperties();
        NamedTypedValue second = getType().getSecondTemplate().copyStructureWithProperties();
        first.setValueBasedOnInstance(((Map.Entry)instance).getKey());
        second.setValueBasedOnInstance(((Map.Entry)instance).getValue());
        setValue(new AbstractMap.SimpleEntry(first, second));
    }

    @Override
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException {
        if (json == null) return;
        if (!(json instanceof Map.Entry))
            throw new IllegalArgumentException("Cannot set value for PairParam with the type:"+json.getClass().getName());
//        assert json instanceof Map.Entry;
        NamedTypedValue first = getType().getFirstTemplate().copyStructureWithProperties();
        NamedTypedValue second = getType().getSecondTemplate().copyStructureWithProperties();
        first.setValueBasedOnInstanceOrJson(((Map.Entry)json).getKey());
        second.setValueBasedOnInstanceOrJson(((Map.Entry)json).getValue());
        setValue(new AbstractMap.SimpleEntry(first, second));
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return super.isValidInstance(instance) || instance instanceof Map.Entry;
    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        return null;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        return null;
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return null;
    }


    @Override
    public List<String> referenceTypes() {
        List<String> references = new ArrayList<>();
        NamedTypedValue template = getType().getFirstTemplate();
        if (template != null){
            references.add(template.getType().getFullTypeName());
            List<String> refrefTypes = template.referenceTypes();
            if (refrefTypes != null)
                references.addAll(refrefTypes);
        }


        template = getType().getSecondTemplate();
        if (template != null){
            references.add(template.getType().getFullTypeName());
            List<String> refrefTypes = template.referenceTypes();
            if (refrefTypes != null)
                references.addAll(refrefTypes);
        }

        if (references.isEmpty()) return null;
        return references;
    }
}
