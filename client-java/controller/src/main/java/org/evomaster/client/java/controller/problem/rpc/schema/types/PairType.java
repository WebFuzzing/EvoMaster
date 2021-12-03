package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.AbstractMap;

/**
 * created by manzhang on 2021/11/27
 */
public class PairType extends TypeSchema{

    public final static String PAIR_TYPE_NAME = AbstractMap.SimpleEntry.class.getSimpleName();
    public final static String FULL_PAIR_TYPE_NAME = AbstractMap.SimpleEntry.class.getName();
    /**
     * template of first
     */
    private final NamedTypedValue firstTemplate;
    /**
     * template of second
     */
    private final NamedTypedValue secondTemplate;

    public PairType(NamedTypedValue keyTemplate, NamedTypedValue valueTemplate) {
        super(PAIR_TYPE_NAME, FULL_PAIR_TYPE_NAME, AbstractMap.SimpleEntry.class);
        this.firstTemplate = keyTemplate;
        this.secondTemplate = valueTemplate;
    }

    public NamedTypedValue getFirstTemplate() {
        return firstTemplate;
    }

    public NamedTypedValue getSecondTemplate() {
        return secondTemplate;
    }
}
