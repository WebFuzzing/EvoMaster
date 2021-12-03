package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PairType;

import java.util.AbstractMap;
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
        return new AbstractMap.SimpleEntry<>(getValue().getKey().newInstance(), getValue().getKey().newInstance());
    }

    @Override
    public PairParam copyStructure() {
        return new PairParam(getType());
    }

    @Override
    public void setValue(ParamDto dto) {
        if (dto.innerContent.size() == 2){
            NamedTypedValue first = getType().getFirstTemplate().copyStructure();
            NamedTypedValue second = getType().getSecondTemplate().copyStructure();
            first.setValue(dto.innerContent.get(0));
            second.setValue(dto.innerContent.get(1));
            setValue(new AbstractMap.SimpleEntry(first, second));
        }
        throw new RuntimeException("ERROR: size of inner content of dto is not 2 for pair type, i.e., "+ dto.innerContent.size());
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        NamedTypedValue first = getType().getFirstTemplate().copyStructure();
        NamedTypedValue second = getType().getSecondTemplate().copyStructure();
        first.setValue(((Map.Entry)instance).getKey());
        second.setValue(((Map.Entry)instance).getValue());
        setValue(new AbstractMap.SimpleEntry(first, second));
    }

    @Override
    public boolean isValidInstance(Object instance) {
        return super.isValidInstance(instance) || instance instanceof Map.Entry;
    }
}
