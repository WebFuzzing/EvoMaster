package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PairType;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * created by manzhang on 2021/11/27
 */
public class PairParam extends NamedTypedValue<PairType, AbstractMap.SimpleEntry<NamedTypedValue, NamedTypedValue>>{
    public final static String PAIR_NAME = "MAP_ENTRY";

    public PairParam(PairType type) {
        super(PAIR_NAME, type);
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
        return new PairParam(getType());
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent.size() == 2){
            NamedTypedValue first = getType().getFirstTemplate().copyStructure();
            NamedTypedValue second = getType().getSecondTemplate().copyStructure();
            first.setValueBasedOnDto(dto.innerContent.get(0));
            second.setValueBasedOnDto(dto.innerContent.get(1));
            setValue(new AbstractMap.SimpleEntry(first, second));
        } else
            throw new RuntimeException("ERROR: size of inner content of dto is not 2 for pair type, i.e., "+ dto.innerContent.size());
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        if (instance == null) return;
        NamedTypedValue first = getType().getFirstTemplate().copyStructure();
        NamedTypedValue second = getType().getSecondTemplate().copyStructure();
        first.setValueBasedOnInstance(((Map.Entry)instance).getKey());
        second.setValueBasedOnInstance(((Map.Entry)instance).getValue());
        setValue(new AbstractMap.SimpleEntry(first, second));
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return super.isValidInstance(instance) || instance instanceof Map.Entry;
    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        return null;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName) {
        return null;
    }

    @Override
    public String getValueAsJavaString() {
        return null;
    }
}
