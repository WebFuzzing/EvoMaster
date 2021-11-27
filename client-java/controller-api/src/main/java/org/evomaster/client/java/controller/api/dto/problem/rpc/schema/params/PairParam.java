package org.evomaster.client.java.controller.api.dto.problem.rpc.schema.params;

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
}
