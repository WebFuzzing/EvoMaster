package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.dto.ParamDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.schema.types.PairType;

import java.util.AbstractMap;

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
}
